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
import java.util.Scanner;

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
		incrementUnit = incrementUnit.toLowerCase();
		switch (incrementUnit){
                        case "daily": date = startDate.plusDays(increment); break;
                        case "weekly": date = startDate.plusDays(increment* 7); break;
			case "biweekly":date = startDate.plusDays(increment* 14); break;
                        case "monthly": date = startDate.plusMonths(increment); break;
			case "quarterly": date = startDate.plusMonths(increment*3); break;
			case "semiannually":date = startDate.plusMonths(increment* 6); break;
                        case "annually": date = startDate.plusYears(increment); break;
                        default: throw new IllegalArgumentException("Invalid increment unit");
                }
		LocalDate tmpSettDate = date;
		if (increment > 0) {
			//date = startDate.plusDays(increment);
			while (
			!calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), leftCurrency, HolidaysLeftCcy)
			|| !calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), rightCurrency, HolidaysRightCcy)
			|| !calendarHoliday.isBusinessDay(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "USD", HolidaysUSD)
			){
				date = date.plusDays(1);
			}
			if((incrementUnit.equals("monthly") ||incrementUnit.equals("quarterly") || incrementUnit.equals("semiannually") || incrementUnit.equals("annullay")) 
				&& date.getMonth()!=tmpSettDate.getMonth()){
				date = getNewBusinessDate(tmpSettDate,-1,"daily",leftCurrency,rightCurrency);
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
		return date;
	}

	public LocalDate getInterDate(LocalDate startDate, int increment, String leftCurrency, String rightCurrency) throws SQLException{
		//LocalDate interDate = startDate;
		int dir = (increment>0) ? 1 : -1;
		LocalDate interDate = startDate.plusDays(1*dir);
		while(increment*dir>0){
			if(leftCurrency != "USD" && !calendarHoliday.isBusinessDay(interDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), leftCurrency, HolidaysLeftCcy)){
				interDate = interDate.plusDays(1*dir);
				continue;
			}
			else if(rightCurrency != "USD" && !calendarHoliday.isBusinessDay(interDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), rightCurrency, HolidaysRightCcy)){
                                interDate = interDate.plusDays(1*dir);
                                continue;
                        }
			else{
				increment -= dir;
			}
		}
		return interDate;
	
	}


	public LocalDate getFixDate(LocalDate startDate, LocalDate valueDate, int increment, String incrementUnit, String source, 
        	String leftCurrency, String rightCurrency, DateRollConvention convention) throws SQLException { 
		LocalDate nextDate = startDate;	
		switch (incrementUnit.toLowerCase()){
            		case "daily": 
				nextDate = startDate.plusDays(increment); 
				while (!calendarHoliday.isBusinessDay(nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
            				if(increment>0){nextDate = nextDate.plusDays(1);}
					else if(increment<0){nextDate = nextDate.minusDays(1);}
				}
				break; 
			case "weekly": 
				nextDate = startDate.plusDays(increment* 7); 
				while (!calendarHoliday.isBusinessDay(nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
            				nextDate = nextDate.plusDays(1);
     				}       
				break;
			case "biweekly":
				nextDate = startDate.plusDays(increment* 14);
                                while (!calendarHoliday.isBusinessDay(nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
                                        nextDate = nextDate.plusDays(1);
                                }
                                break;
			case "monthly": 
			case "annually": 
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
	
	public LocalDate getValueDate(LocalDate tradeDate, int settDays, String leftCurrency, String rightCurrency) throws SQLException {
    		LocalDate date = tradeDate;
		if(settDays>0){
			while(settDays>1){
				date = getInterDate(date,  1, leftCurrency, rightCurrency);
    				//date = getNewBusinessDate(date, 1, "Daily", leftCurrency,rightCurrency);
    				settDays -= 1;
    			}
			date = getNewBusinessDate(date, 1, "Daily", leftCurrency,rightCurrency);
			settDays -=1;
		}
		else if(settDays<0){
			while (settDays<0){
				date = getInterDate(date,  -1, leftCurrency, rightCurrency);
				//date = getNewBusinessDate(date, -1, "Daily", leftCurrency,rightCurrency);
				settDays += 1;
			}
			date = getNewBusinessDate(date, -1, "Daily", leftCurrency,rightCurrency);
		}

		return date;
    }


    public LocalDate [] getFixingSettDates(LocalDate tradeDate, LocalDate valueDate, int tenor, String  tenorUnit, int settDays, String leftCurrency, String rightCurrency, String source) throws SQLException { 
    	LocalDate dates[] = new LocalDate[2];
    	LocalDate fixingDate = null;
	LocalDate settDate = null;

    	switch(tenorUnit.toLowerCase()){
    		case "daily":
    			fixingDate = tradeDate.plusDays(tenor);
                	while (!calendarHoliday.isBusinessDay(fixingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
            			if(tenor>0){      fixingDate = fixingDate.plusDays(1);}
                		else if(tenor<0){ fixingDate = fixingDate.minusDays(1);}
     			}
     			settDate = getValueDate(fixingDate, settDays,leftCurrency,rightCurrency);
     			break;

    		case "weekly":
		case "biweekly":
			tenor = (tenorUnit.toLowerCase().equals("weekly")) ? tenor*7 : tenor*14;
			fixingDate = tradeDate.plusDays(tenor); 
                	while (!calendarHoliday.isBusinessDay(fixingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
            			fixingDate = fixingDate.plusDays(1);
     			}
     			settDate = getValueDate(fixingDate, settDays,leftCurrency,rightCurrency);
     			break;

    		case "monthly":
		case "quarterly":
		case "semiannually":
    		case "annually":
    			settDate = getNewBusinessDate(valueDate, tenor, tenorUnit, leftCurrency, rightCurrency);
    			while(settDays>1){
    				fixingDate = getInterDate(settDate, -1, leftCurrency,rightCurrency);
    				settDays -= 1;
			}
			fixingDate = (fixingDate!=null) ? fixingDate.plusDays(-1) : settDate.plusDays(-1);
    			while (!calendarHoliday.isBusinessDay(fixingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), source, HolidaysSource)) {
            			fixingDate = fixingDate.plusDays(-1);
     			}
    	}
    	dates[0] = fixingDate;
    	dates[1] = settDate;
    	return dates;
    }

	private LocalDate getSettDate(LocalDate startDate, int increment,String incrementUnit,  DateRollConvention convention, String 
	leftCurrency, String rightCurrency) throws SQLException { 
		LocalDate date = startDate; switch (convention) {
            	case FOLLOWING: 
			//while (increment > 0) { 
				date = getNewBusinessDate(date, increment,incrementUnit, leftCurrency, rightCurrency); 
                    		increment -= 1;
                	//}	
                	break; 
		case MODIFIED_FOLLOWING: 
			//int tempInc = increment; 
			//while (tempInc > 0) { 
				date = getNewBusinessDate(date, 1, incrementUnit,leftCurrency, rightCurrency); 
				//tempInc -= 1;                	
				//}
                	if (date.getMonth() != startDate.getMonth()) { 
				date = getSettDate(startDate, increment, incrementUnit, DateRollConvention.PREVIOUS, leftCurrency, rightCurrency);
                	}
               		break; 
		case PREVIOUS: 
			//while (increment > 0) { 
				date = getNewBusinessDate(date, -1, incrementUnit, leftCurrency, rightCurrency); 
			//increment -= 1;
                	//}
                	break; 
		case MODIFIED_PREVIOUS: 
			//tempInc = increment; 
			//while (tempInc > 0) { 
				date = getNewBusinessDate(date, -1, incrementUnit, leftCurrency, rightCurrency); 
				//tempInc -= 1;
                	//}
                	if (date.getMonth() != startDate.getMonth()) { 
				date = getSettDate(startDate, increment, incrementUnit, DateRollConvention.FOLLOWING, leftCurrency, rightCurrency);
                	}
                	break;
        }
        return date;
    }

    	public List<LocalDate[]> generateSchedule(
			LocalDate startDate,LocalDate valueDate, 
			String fixingFrequency, String settlementFrequency, int noOfFixings, int settDays,
			String source,String leftCurrency, String rightCurrency, DateRollConvention dateRollConvention) throws SQLException {
		List<LocalDate[]> listOfFixSett = new ArrayList<>();
		//LocalDate prevFixDate = startDate;
		//LocalDate prevSettDate = valueDate;
		LocalDate firstFixSett[] = getFixingSettDates(startDate, valueDate, 1, fixingFrequency, settDays, leftCurrency, rightCurrency, source);
		listOfFixSett.add(firstFixSett);
		LocalDate fixSet[]  = new LocalDate[2]; 
		//LocalDate nextFixingDate = startDate;
		for(int i=1; i< noOfFixings; i++){
			//nextFixingDate = getFixDate(nextFixingDate, null, 1, fixingFrequency, source,leftCurrency, rightCurrency,dateRollConvention ); //1, fixingFrequency, source);
			//listOfFixings.add(nextFixingDate);
			fixSet = getFixingSettDates(firstFixSett[0], firstFixSett[1], i, fixingFrequency, settDays, leftCurrency, rightCurrency, source);
			listOfFixSett.add(fixSet);
			//prevFixDate = fixSet[0];
			//prevSettDate = fixSet[1];
			
	           }
		return listOfFixSett;
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
            		Scanner scanner = new Scanner(System.in);

			Schedule schedule = new Schedule(dbUrl, user, password); 
			LocalDate startDate = LocalDate.parse("2024-07-26"); 
            		System.out.println("Trade Date: "+ startDate);
			System.out.print("Left Currency: "); String leftCurrency = scanner.nextLine().toUpperCase();
			System.out.print("Right Currency: "); String rightCurrency = scanner.nextLine().toUpperCase();
			System.out.print("Fixing/Sett Frequency: "); String freq = scanner.nextLine();
			System.out.print("No of Fixings: "); int increment = scanner.nextInt();
			//int increment = 10; 
			//String leftCurrency = "GBP"; 
			//String rightCurrency = "EUR"; 
			DateRollConvention dateRollConvention = DateRollConvention.FOLLOWING; 
			String source = "BFIX"; 
			//String freq = "BiWeekly";
			int settDays = 2;

			schedule.getHolidays(startDate, 365,leftCurrency,rightCurrency,source); 

			LocalDate valueDate = schedule.getValueDate(startDate, settDays, leftCurrency, rightCurrency);
                        System.out.println("\nValue Date: " + valueDate);
			
			LocalDate[] fixSettDate = schedule.getFixingSettDates(startDate,valueDate, increment, freq, settDays ,leftCurrency, rightCurrency,source ); 
			System.out.println("For Single Fixing --> Fixing Date: " + fixSettDate[0]+", Sett Date: "+ fixSettDate[1]); 
			
			
			List<LocalDate[]> fixings = schedule.generateSchedule(startDate,valueDate, freq,freq,increment,settDays, source,leftCurrency, rightCurrency,dateRollConvention);
			System.out.println("For multi fixing schedule -");
			for (int i = 0; i < fixings.size(); i++) {
				System.out.println("Period: "+(i+1)+", Fixing Date: "+fixings.get(i)[0]+", Settlement Date: "+fixings.get(i)[1]);
			}
			
			scanner.close();
			schedule.close();
        	} catch (SQLException e) {
            		e.printStackTrace();
        	}
    	}
}


