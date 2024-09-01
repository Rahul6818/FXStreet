import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.time.DayOfWeek; 
import java.time.LocalDate; 
import java.time.format.DateTimeFormatter; 
import java.util.Arrays; 
import java.util.HashSet; 
import java.util.Set; 

public class CalendarHoliday {
    private Connection conn; public CalendarHoliday(String dbUrl, String user, String password) throws SQLException { 
        conn = DriverManager.getConnection(dbUrl, user, password);
    }
    public boolean isHoliday(String date, String calendarName, List<Date> ListOfHolidays) throws SQLException {
        
        if(ListOfHolidays = null){ String query = "SELECT COUNT(*) FROM Holidays WHERE date = ? AND calendar_name = ?"; 
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, date); pstmt.setString(2, calendarName); try (ResultSet rs = pstmt.executeQuery()) { 
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
            return false;
        }
        return ListOfHolidays.contains(date);
    }
    public List<Date> GetHolidaysInRange(String startDate,String endDate, String calendarName) throws SQLException {
        // Convert the date string to a LocalDate object
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); LocalDate startLocalDate = 
        LocalDate.parse(startDate, formatter); LocalDate endLocalDate = LocalDate.parse(endDate, formatter); String query 
        = "SELECT date FROM Holidays WHERE (date BETWEEN ? AND ?) AND Calendar = ?";
        // List to hold holiday dates
        List<Date> holidayDates = new ArrayList<>(); try (Connection conn = DriverManager.getConnection(url, user, 
        password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            // Set parameters
            pstmt.setString(1, startLocalDate); pstmt.setString(2, endLocalDate); pstmt.setString(3, calendarName);
            // Execute query
            ResultSet rs = pstmt.executeQuery();
            // Process result set
            while (rs.next()) { holidayDates.add(rs.getDate("holiday_date"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return holidayDates;
    }
    public boolean isBusinessDay(String date, String calendarName, List<Date> ListOfHolidays) throws SQLException {
        // Check if the date is a holiday
        if (isHoliday(date, calendarName, ListOfHolidays)) { return false;
        }
        // Convert the date string to a LocalDate object
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); LocalDate localDate = 
        LocalDate.parse(date, formatter);
        
        // Check if the date is a weekend
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        // Non-standard weekend countries
        Set<String> nonStandardWeekendCountries = new HashSet<>(Arrays.asList( 
				"DZD", /* Algerian Dinar */ "Algiers Stock Exchange", 
				"AFN", /* Afghan Afghani */ "Afghanistan Stock Exchange", // ASE 
				"BHD", /* Bahraini Dinar */ "Bahrain Bourse", 
				"BDT", /* Bangladeshi Taka */ "Dhaka Stock Exchange" , //DSE 
				"EFP", /* Egyptian Pound */ "Egyptian Exchange", // EGX 
				"IRR", /* Iranian Rial */ "Tehran Stock Exchange", // TSE 
				"ILS", /* Israeli New Shekel */ "Tel Aviv Stock Exchange", // TASE 
				"JOD", /* Jordanian Dinar */ "Amman Stock Exchange" // ASE
        ));
        
        // Check calendar name for country-specific weekend rules
        if (nonStandardWeekendCountries.contains(calendarName)) { if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == 
            DayOfWeek.SATURDAY) {
                return false;
            }
        } else {
            // Default weekend: Saturday and Sunday
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) { return false;
            }
        }
        return true;
    }
    public void close() throws SQLException { if (conn != null && !conn.isClosed()) { conn.close();
        }
    }
    public static void main(String[] args) { 
	    String dbUrl = "jdbc:postgresql://localhost:5432/ubuntu"; 
	    String user = "ubuntu"; 
	    String password = "12345"; 
	    try {
            	CalendarHoliday calendarHoliday = new CalendarHoliday(dbUrl, user, password); 
		boolean isHoliday = calendarHoliday.isHoliday("2024-12-25", "US_Holidays"); 
		System.out.println("Is Holiday: " + isHoliday); // True if the date is a holiday, otherwise False 
		boolean isBusinessDay = calendarHoliday.isBusinessDay("2024-12-25", "US_Holidays"); 
		System.out.println("Is Business Day: " + isBusinessDay); // True if the date is a business day, otherwise False 
		calendarHoliday.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
