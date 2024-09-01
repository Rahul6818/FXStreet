import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.SQLException; 
import java.time.DayOfWeek; 
import java.time.LocalDate; 
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList; 
import java.util.List; 
import java.util.Arrays; 
import java.util.HashSet; 
import java.util.Set; 
/*
public enum DateRollConvention {
    FOLLOWING, MODIFIED_FOLLOWING, PREVIOUS, MODIFIED_PREVIOUS
}

public enum Frequency { 
	DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, SEMIANNUALLY, ANNUALLY
}
*/
public class Schedule {
	private Connection conn;
	private CalendarHoliday calendarHoliday;
	private List<LocalDate> HolidaysLeftCcy;
	private List<LocalDate> HolidaysRightCcy;
	private List<LocalDate> HolidaysSource;
	private List<LocalDate> HolidaysUSD;

	public Schedule(String dbUrl, String user, String password) throws SQLException {
		//conn = DriverManager.getConnection(dbUrl, user, password);
		calendarHoliday = new CalendarHoliday(dbUrl,user,password);
		HolidaysLeftCcy = new ArrayList<>();
		HolidaysRightCcy = new ArrayList<>();
		HolidaysSource = new ArrayList<>();
		HolidaysUSD = new ArrayList<>();
	}

	public void getHolidays(LocalDate startDate, int increment, String leftCurrency, String rightCurrency, String 
	source) throws SQLException { 
		LocalDate endDate = startDate.plusDays(increment + 30); 
		HolidaysLeftCcy = calendarHoliday.getHolidaysInRange(startDate.toString(), endDate.toString(), leftCurrency); 
		HolidaysRightCcy = calendarHoliday.getHolidaysInRange(startDate.toString(), endDate.toString(), rightCurrency); 
		HolidaysUSD = calendarHoliday.getHolidaysInRange(startDate.toString(), endDate.toString(), "USD"); 
		HolidaysSource = calendarHoliday.getHolidaysInRange(startDate.toString(), endDate.toString(), source);
	}

	public LocalDate getNewBusinessDate(LocalDate startDate, int increment,String incrementUnit, String leftCurrency, String rightCurrency) throws SQLException {
		LocalDate date;
		switch (incrementUnit){
                        case "Daily": date = startDate.plusDays(increment); break;
                        case "Weekly": date = startDate.plusDays(increment* 7); break;
                        case "Monthly": date = startDate.plusMonths(increment); break;
                        case "Yearly": date = startDate.plusYears(increment); break;
                        default: throw new IllegalArgumentException("Invalid increment unit");
                }
		
		if (increment > 0) {
			//date = startDate.plusDays(increment);
			while (
			!calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), leftCurrency, HolidaysLeftCcy)
			|| !calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), rightCurrency, HolidaysRightCcy)
			|| !calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "USD", HolidaysUSD)
			){
				//System.out.println("Sett Date Cal: "+date);
				date = date.plusDays(1);
			}
		}
		else if (increment<0){
			//date = startDate.minusDays(1);
			while (
				!calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), leftCurrency, HolidaysLeftCcy)
				|| !calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), rightCurrency, HolidaysRightCcy)
				|| !calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "USD", HolidaysUSD)
			){
				date = date.minusDays(1);
			}
		}
		System.out.println("Sett Date Cal: "+date);
		return date;
	}
	
	public LocalDate getFixDate(LocalDate startDate, LocalDate valueDate, int increment, String incrementUnit, String source, 
        	String leftCurrency, String rightCurrency, DateRollConvention convention) throws SQLException { 
		LocalDate nextDate = startDate; 
		switch (incrementUnit){
            		case "Daily": 
				nextDate = startDate.plusDays(increment); 
				while (!calendarHoliday.isBusinessDay(nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
            				if(increment>0){nextDate = nextDate.plusDays(1);}
					else if(increment<0){nextDate = nextDate.minusDays(1);}
				}
				break; 
			case "Weekly": 
				nextDate = startDate.plusDays(increment* 7); 
				while (!calendarHoliday.isBusinessDay(nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
            				nextDate = nextDate.plusDays(1);
     				}       
				break;
			case "Monthly": 
			case "Yearly": 
				LocalDate settDate = getSettDate(valueDate,increment,incrementUnit,convention, leftCurrency,rightCurrency);
                        	System.out.println(valueDate +", Monthly Delivery Date: "+ settDate);
				nextDate = getSettDate(settDate,1,"Daily",DateRollConvention.PREVIOUS, leftCurrency,rightCurrency);
				System.out.println("Monthly T - 1 Date: "+ nextDate);
                        	nextDate = getFixDate(nextDate, null, -1, "Daily", source,leftCurrency,rightCurrency, convention) ; 
				break;
            		default: throw new IllegalArgumentException("Invalid increment unit");
        	}
        /*
	while (!calendarHoliday.isBusinessDay(nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 
        source, HolidaysSource)) {
            nextDate = nextDate.plusDays(1);
        }*/
        return nextDate;
    }
	
	private LocalDate getSettDate(LocalDate startDate, int increment,String incrementUnit,  DateRollConvention convention, String 
	leftCurrency, String rightCurrency) throws SQLException { 
		LocalDate date = startDate; switch (convention) {
            	case FOLLOWING: 
			while (increment > 0) { 
				date = getNewBusinessDate(date, 1,incrementUnit, leftCurrency, rightCurrency); 
                    		increment -= 1;
                	}	
                	break; 
		case MODIFIED_FOLLOWING: 
			int tempInc = increment; 
			while (tempInc > 0) { 
				date = getNewBusinessDate(date, 1, incrementUnit,leftCurrency, rightCurrency); tempInc -= 1;
                	}
                	if (date.getMonth() != startDate.getMonth()) { 
				date = getSettDate(startDate, increment, incrementUnit, DateRollConvention.PREVIOUS, leftCurrency, rightCurrency);
                	}
               		break; 
		case PREVIOUS: 
			while (increment > 0) { 
				date = getNewBusinessDate(date, -1, incrementUnit, leftCurrency, rightCurrency); 
			increment -= 1;
                	}
                	break; 
		case MODIFIED_PREVIOUS: 
			tempInc = increment; 
			while (tempInc > 0) { 
				date = getNewBusinessDate(date, -1, incrementUnit, leftCurrency, rightCurrency); 
				tempInc -= 1;
                	}
                	if (date.getMonth() != startDate.getMonth()) { 
				date = getSettDate(startDate, increment, incrementUnit, DateRollConvention.FOLLOWING, leftCurrency, rightCurrency);
                	}
                	break;
        }
        return date;
    }

    	public List<LocalDate> generateSchedule(LocalDate startDate, String fixingFrequency, String settlementFrequency, int noOfFixings, String source,String leftCurrency, String rightCurrency, DateRollConvention dateRollConvention) 
			throws SQLException {
		List<LocalDate> listOfFixings = new ArrayList<>();
		LocalDate nextFixingDate = startDate;
		while (noOfFixings > 0) {
			nextFixingDate = getFixDate(nextFixingDate, null, 1, fixingFrequency, source,leftCurrency, rightCurrency,dateRollConvention ); //1, fixingFrequency, source);
			listOfFixings.add(nextFixingDate);
			noOfFixings--;
	           }
		return listOfFixings;
	}

    	public void close() throws SQLException {
		if (conn != null && !conn.isClosed()) {
			conn.close();
	            }
	}
	
	public static void main(String[] args) { 
		String dbUrl = "jdbc:postgresql://localhost:5432/ubuntu"; 
		String user = "ubuntu"; 
		String password = "ubuntu"; 
		try {
            		Schedule schedule = new Schedule(dbUrl, user, password); 
			LocalDate startDate = LocalDate.parse("2024-07-25"); 
            		int increment = 5; 
			String leftCurrency = "USD"; 
			String rightCurrency = "EUR"; 
			DateRollConvention dateRollConvention = DateRollConvention.FOLLOWING; 
			String source = "BFIX"; 
			
			schedule.getHolidays(startDate, 365,leftCurrency,rightCurrency,source); 

			LocalDate settDate = schedule.getSettDate(startDate, 2,"Daily", dateRollConvention, leftCurrency, rightCurrency);
                        System.out.println("Settlement Date: " + settDate);
			
			LocalDate fixDate = schedule.getFixDate(startDate,settDate, increment, "Monthly", source,leftCurrency, rightCurrency,dateRollConvention ); 
			System.out.println("Fixing Date: " + fixDate); 
			
			/*
			List<LocalDate> fixings = schedule.generateSchedule(startDate,null, increment, "Weekly", source,leftCurrency, rightCurrency,dateRollConvention);

			for (int i = 0; i < fixings.size(); i++) {
				System.out.println(fixings.get(i));
			}
			*/
			schedule.close();
        	} catch (SQLException e) {
            		e.printStackTrace();
        	}
    	}
}
