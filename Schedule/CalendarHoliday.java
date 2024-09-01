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
import java.util.List; 
import java.util.Set; 
import java.util.ArrayList; 

public class CalendarHoliday {
    private Connection conn; public CalendarHoliday(String dbUrl, String user, String password) throws SQLException { 
        try {
		Class.forName("org.postgresql.Driver");
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
		throw new SQLException("PostgreSQL JDBC Driver not found.", e);
	}
	conn = DriverManager.getConnection(dbUrl, user, password);
    }
    public boolean isHoliday(String date, String calendarName, List<LocalDate> listOfHolidays) throws SQLException { if 
        (listOfHolidays == null) {
            String query = "SELECT COUNT(*) FROM Holidays WHERE date = TO_DATE(?,'yyyy-MM-dd') AND calendar = ?"; 
	    try (PreparedStatement 
            pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, date); pstmt.setString(2, calendarName); try (ResultSet rs = pstmt.executeQuery()) { 
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
            return false;
        }
        return listOfHolidays.contains(LocalDate.parse(date));
    }
    public List<LocalDate> getHolidaysInRange(String startDate, String endDate, String calendarName) throws SQLException 
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); LocalDate startLocalDate = 
        LocalDate.parse(startDate, formatter); LocalDate endLocalDate = LocalDate.parse(endDate, formatter); String query 
        = "SELECT date FROM Holidays WHERE (date BETWEEN TO_DATE(?,'yyyy-MM-dd') AND TO_DATE(?,'yyyy-MM-dd')) AND calendar = ?"; 
	List<LocalDate> holidayDates = 
        new ArrayList<>(); try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startLocalDate.toString()); pstmt.setString(2, endLocalDate.toString()); 
            pstmt.setString(3, calendarName); try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) { holidayDates.add(LocalDate.parse(rs.getString("date"), formatter));
                }
            }
        }
        return holidayDates;
    }
    public boolean isBusinessDay(String date, String calendarName, List<LocalDate> listOfHolidays) throws SQLException { 
	    if (isHoliday(date, calendarName, listOfHolidays)) {
            	return false;
        	}
        
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); 
	    LocalDate localDate = LocalDate.parse(date, formatter); 
	    DayOfWeek dayOfWeek = localDate.getDayOfWeek(); 
	    Set<String> nonStandardWeekendCountries = new HashSet<>(Arrays.asList(
            "DZD", "AFN", "BHD", "BDT", "EGP", "IRR", "ILS", "JOD" )); 
	    if (nonStandardWeekendCountries.contains(calendarName)) {
            	if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) { return false;}
        	} 
	    else {
            	if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) { return false;}
        	}
        return true;
    }
    public void close() throws SQLException { if (conn != null && !conn.isClosed()) { conn.close();
        }
    }
    public static void main(String[] args) { 
	    String dbUrl = "jdbc:postgresql://localhost:5432/ubuntu"; 
	    String user = "ubuntu"; 
	    String password = "123456789"; 
	    try {
            	CalendarHoliday calendarHoliday = new CalendarHoliday(dbUrl, user, password); 
		boolean isHoliday = calendarHoliday.isHoliday("2024-07-25", "USD", null); 
		System.out.println("Is Holiday: " + isHoliday); 
            	boolean isBusinessDay = calendarHoliday.isBusinessDay("2024-07-26", "EUR", null); 
            	System.out.println("Is Business Day: " + isBusinessDay); 
		calendarHoliday.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
