package JDBC;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
 
public class MovieTicketBooking {
    static final Scanner sc = new Scanner(System.in);
    static final int MAX_SEATS_PER_BOOKING = 6;
    static final int CLEANING_GAP_MIN = 15;   // gap needed between two shows in the same theater
 
    
    static class DBConnection {
        private static final String URL = "jdbc:mysql://localhost:3306/moviedb";
        private static final String USER = "root";
        private static final String PASSWORD = "Krishna@333";
 
        static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }
    }
 
    
    public static void main(String[] args) {
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("Connected to MySQL successfully.");
            while (true) {
                System.out.println("\n=== MOVIE TICKET BOOKING SYSTEM ===");
                System.out.println("1. Add movie\n2. View movies\n3. Add theater\n4. View theaters\n5. Delete theater");
                System.out.println("6. Add show\n7. View shows\n8. View seat map\n9. Book tickets\n10. Print ticket\n0. Exit");
                int choice = readInt("Choice: ", 0, 10);
                try {
                    switch (choice) {
                        case 1: addMovie(con); break;
                        case 2: viewMovies(con); break;
                        case 3: addTheater(con); break;
                        case 4: viewTheaters(con); break;
                        case 5: deleteTheater(con); break;
                        case 6: addShow(con); break;
                        case 7: viewShows(con); break;
                        case 8: seatMap(con, readInt("Show ID: ", 1, Integer.MAX_VALUE)); break;
                        case 9: bookTickets(con); break;
                        case 10: printTicket(con, readTicketRef()); break;
                        case 0: System.out.println("Goodbye!"); return;
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Validation error: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Database operation failed: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        } finally {
            sc.close();
        }
    }
 
    
    static String readText(String label, int maxLength) {
        while (true) {
            System.out.print(label);
            String value = sc.nextLine().trim();
            if (!value.isEmpty() && value.length() <= maxLength) return value;
            System.out.println("Required: 1 to " + maxLength + " characters.");
        }
    }
 
    static String readLetters(String label, int maxLength) {
        while (true) {
            String value = readText(label, maxLength);
            if (value.matches("[\\p{L} .'-]{2,}")) return value;
            System.out.println("Use letters (and spaces . ' -) only, minimum 2 characters.");
        }
    }
 
    static int readInt(String label, int min, int max) {
        while (true) {
            System.out.print(label);
            try {
                int value = Integer.parseInt(sc.nextLine().trim());
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a whole number from " + min + " to " + max + ".");
        }
    }
 
    static double readPrice() {
        while (true) {
            System.out.print("Ticket price (1-5000): ");
            try {
                double n = Double.parseDouble(sc.nextLine().trim());
                if (Double.isFinite(n) && n >= 1 && n <= 5000 && Math.round(n * 100) / 100.0 == n) return n;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a price from 1 to 5000 with at most 2 decimal places.");
        }
    }
 
    static String readPhone() {
        while (true) {
            String phone = readText("Phone (10 digits): ", 10);
            if (phone.matches("[6-9][0-9]{9}")) return phone;
            System.out.println("Enter a valid 10-digit Indian mobile number.");
        }
    }
 
    static LocalDate readShowDate() {
        while (true) {
            String raw = readText("Show date (YYYY-MM-DD): ", 10);
            try {
                LocalDate d = LocalDate.parse(raw);
                if (d.isBefore(LocalDate.now())) System.out.println("Show date cannot be in the past.");
                else if (d.isAfter(LocalDate.now().plusDays(90))) System.out.println("Shows can be scheduled only 90 days ahead.");
                else return d;
            } catch (DateTimeParseException e) {
                System.out.println("Enter a real date in YYYY-MM-DD format.");
            }
        }
    }
 
    static LocalTime readShowTime() {
        while (true) {
            String raw = readText("Show time (HH:mm, 24-hour): ", 5);
            if (raw.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) return LocalTime.parse(raw);
            System.out.println("Enter time like 18:30.");
        }
    }
 
    static String readTicketRef() {
        while (true) {
            String ref = readText("Ticket reference (e.g. TKT-1A2B3C4D): ", 12).toUpperCase();
            if (ref.matches("TKT-[0-9A-F]{8}")) return ref;
            System.out.println("Invalid reference format.");
        }
    }
 
    static boolean exists(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
 
    
    static void addMovie(Connection con) throws SQLException {
        String title = readText("Title: ", 100);
        String genre = readLetters("Genre: ", 30);
        String language = readLetters("Language: ", 30);
        int duration = readInt("Duration in minutes (30-300): ", 30, 300);
        String sql = "INSERT INTO movies(title,genre,movie_language,duration_min) VALUES(?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title); ps.setString(2, genre); ps.setString(3, language); ps.setInt(4, duration);
            ps.executeUpdate();
            System.out.println("Movie added.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("This movie already exists in this language.");
        }
    }
 
    static void viewMovies(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM movies ORDER BY movie_id");
             ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-5s %-30s %-12s %-12s %-8s%n", "ID", "Title", "Genre", "Language", "Minutes");
            while (rs.next())
                System.out.printf("%-5d %-30s %-12s %-12s %-8d%n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5));
        }
    }
 
    
    static void addTheater(Connection con) throws SQLException {
        String name = readText("Theater name: ", 60);
        String city = readLetters("City: ", 40);
        int rows = readInt("Number of rows (1-26, rows are A, B, C...): ", 1, 26);
        int cols = readInt("Seats per row (1-30): ", 1, 30);
        String sql = "INSERT INTO theaters(name,city,total_rows,seats_per_row) VALUES(?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, city); ps.setInt(3, rows); ps.setInt(4, cols);
            ps.executeUpdate();
            System.out.println("Theater added. Capacity: " + rows * cols + " seats.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("A theater with this name already exists in this city.");
        }
    }
 
    static void viewTheaters(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM theaters ORDER BY theater_id");
             ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-5s %-25s %-15s %-6s %-10s %-8s%n", "ID", "Name", "City", "Rows", "Seats/Row", "Capacity");
            while (rs.next())
                System.out.printf("%-5d %-25s %-15s %-6d %-10d %-8d%n", rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(4) * rs.getInt(5));
        }
    }
 
    static void deleteTheater(Connection con) throws SQLException {
        int id = readInt("Theater ID to delete: ", 1, Integer.MAX_VALUE);
        if (!exists(con, "SELECT 1 FROM theaters WHERE theater_id=?", id)) { System.out.println("Theater not found."); return; }
        if (exists(con, "SELECT 1 FROM shows WHERE theater_id=?", id)) {
            System.out.println("Cannot delete: this theater has shows scheduled."); return;
        }
        if (!readText("Type YES to confirm deletion: ", 10).equals("YES")) { System.out.println("Cancelled."); return; }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM theaters WHERE theater_id=?")) {
            ps.setInt(1, id);
            System.out.println(ps.executeUpdate() + " theater deleted.");
        }
    }
 
    
    static void addShow(Connection con) throws SQLException {
        int movieId = readInt("Movie ID: ", 1, Integer.MAX_VALUE);
        int theaterId = readInt("Theater ID: ", 1, Integer.MAX_VALUE);
 
        int duration = -1;
        try (PreparedStatement ps = con.prepareStatement("SELECT duration_min FROM movies WHERE movie_id=?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) duration = rs.getInt(1); }
        }
        if (duration < 0) { System.out.println("Movie not found."); return; }
        if (!exists(con, "SELECT 1 FROM theaters WHERE theater_id=?", theaterId)) { System.out.println("Theater not found."); return; }
 
        LocalDate date = readShowDate();
        LocalTime time = readShowTime();
        if (date.equals(LocalDate.now()) && !time.isAfter(LocalTime.now())) {
            System.out.println("Show time must be in the future."); return;
        }
        double price = readPrice();
 
        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM theaters WHERE theater_id=? FOR UPDATE")) {
                ps.setInt(1, theaterId); ps.executeQuery().close();
            }
            int newStart = time.getHour() * 60 + time.getMinute();
            int newEnd = newStart + duration + CLEANING_GAP_MIN;
            String sql = "SELECT s.show_time, m.duration_min FROM shows s JOIN movies m ON m.movie_id=s.movie_id " +
                         "WHERE s.theater_id=? AND s.show_date=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, theaterId); ps.setDate(2, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalTime t = rs.getTime(1).toLocalTime();
                        int oldStart = t.getHour() * 60 + t.getMinute();
                        int oldEnd = oldStart + rs.getInt(2) + CLEANING_GAP_MIN;
                        if (newStart < oldEnd && oldStart < newEnd) {
                            System.out.println("Clash: another show runs in this theater from " + t + " (needs " + CLEANING_GAP_MIN + " min gap).");
                            con.rollback();
                            return;
                        }
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO shows(movie_id,theater_id,show_date,show_time,price) VALUES(?,?,?,?,?)")) {
                ps.setInt(1, movieId); ps.setInt(2, theaterId); ps.setDate(3, Date.valueOf(date));
                ps.setTime(4, Time.valueOf(time)); ps.setDouble(5, price);
                ps.executeUpdate();
            }
            con.commit();
            System.out.println("Show added.");
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }
 
    static void viewShows(Connection con) throws SQLException {
        String sql = "SELECT s.show_id, m.title, t.name, t.city, s.show_date, s.show_time, s.price, " +
                     "t.total_rows*t.seats_per_row - (SELECT COUNT(*) FROM bookings b WHERE b.show_id=s.show_id) AS available " +
                     "FROM shows s JOIN movies m ON m.movie_id=s.movie_id JOIN theaters t ON t.theater_id=s.theater_id " +
                     "WHERE s.show_date >= CURDATE() ORDER BY s.show_date, s.show_time";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-5s %-22s %-20s %-12s %-6s %-8s %-9s%n", "ID", "Movie", "Theater", "Date", "Time", "Price", "Available");
            while (rs.next())
                System.out.printf("%-5d %-22s %-20s %-12s %-6s %-8.2f %-9d%n", rs.getInt(1), rs.getString(2),
                        rs.getString(3) + ", " + rs.getString(4), rs.getDate(5), rs.getTime(6).toString().substring(0, 5),
                        rs.getDouble(7), rs.getInt(8));
        }
    }
 

    static Set<String> takenSeats(Connection con, int showId) throws SQLException {
        Set<String> taken = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT seat_number FROM bookings WHERE show_id=?")) {
            ps.setInt(1, showId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) taken.add(rs.getString(1)); }
        }
        return taken;
    }
 
    
    static boolean seatMap(Connection con, int showId) throws SQLException {
        int rows, cols;
        String sql = "SELECT t.total_rows, t.seats_per_row FROM shows s JOIN theaters t ON t.theater_id=s.theater_id WHERE s.show_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, showId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Show not found."); return false; }
                rows = rs.getInt(1); cols = rs.getInt(2);
            }
        }
        Set<String> taken = takenSeats(con, showId);
        System.out.println("\n" + String.format("%" + (cols * 2) + "s", "") + "[ SCREEN ]");
        System.out.print("    ");
        for (int c = 1; c <= cols; c++) System.out.printf("%3d", c);
        System.out.println();
        for (int r = 0; r < rows; r++) {
            char rowLetter = (char) ('A' + r);
            System.out.print(" " + rowLetter + "  ");
            for (int c = 1; c <= cols; c++) System.out.printf("%3s", taken.contains("" + rowLetter + c) ? "X" : ".");
            System.out.println();
        }
        System.out.println("(. = free, X = booked)");
        return true;
    }
 
    
    static List<String> parseSeats(String raw) {
        List<String> seats = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String part : raw.split(",")) {
            String s = part.trim().toUpperCase();
            if (!s.matches("[A-Z][0-9]{1,2}"))
                throw new IllegalArgumentException("Invalid seat '" + part.trim() + "'. Use a row letter and seat number, e.g. A1, B12.");
            s = s.charAt(0) + String.valueOf(Integer.parseInt(s.substring(1)));
            if (!seen.add(s)) throw new IllegalArgumentException("Seat " + s + " is entered more than once.");
            seats.add(s);
        }
        if (seats.size() > MAX_SEATS_PER_BOOKING)
            throw new IllegalArgumentException("You can book at most " + MAX_SEATS_PER_BOOKING + " seats at a time.");
        return seats;
    }
 
    static void bookTickets(Connection con) throws SQLException {
        int showId = readInt("Show ID: ", 1, Integer.MAX_VALUE);
        if (!seatMap(con, showId)) return;
        String name = readLetters("Customer name: ", 50);
        String phone = readPhone();
        List<String> seats = parseSeats(readText("Seats (comma separated, e.g. A1,A2): ", 40));
 
        String ref = bookSeats(con, showId, name, phone, seats);
        if (ref == null) {
            System.out.println("Booking failed: one of the seats was just taken by someone else. Nothing was booked. Please try again.");
            return;
        }
        System.out.println("Booking successful!");
        printTicket(con, ref);
    }
 
   
    static String bookSeats(Connection con, int showId, String name, String phone, List<String> seats) throws SQLException {
        boolean oldAutoCommit = con.getAutoCommit();
        String ref = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            con.setAutoCommit(false);
 
            int rows = 0, cols = 0;
            double price = 0;
            LocalDateTime start = null;
            String lockSql = "SELECT s.price, s.show_date, s.show_time, t.total_rows, t.seats_per_row " +
                             "FROM shows s JOIN theaters t ON t.theater_id=s.theater_id WHERE s.show_id=? FOR UPDATE";
            try (PreparedStatement ps = con.prepareStatement(lockSql)) {
                ps.setInt(1, showId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Show not found.");
                    price = rs.getDouble(1);
                    start = LocalDateTime.of(rs.getDate(2).toLocalDate(), rs.getTime(3).toLocalTime());
                    rows = rs.getInt(4); cols = rs.getInt(5);
                }
            }
            if (!start.isAfter(LocalDateTime.now()))
                throw new IllegalArgumentException("This show has already started or finished.");
 
            for (String seat : seats) {
                int rowIndex = seat.charAt(0) - 'A';
                int number = Integer.parseInt(seat.substring(1));
                if (rowIndex >= rows || number < 1 || number > cols)
                    throw new IllegalArgumentException("Seat " + seat + " does not exist. This theater has rows A-"
                            + (char) ('A' + rows - 1) + " and seats 1-" + cols + " in each row.");
            }
 
            Set<String> taken = takenSeats(con, showId);
            List<String> clash = new ArrayList<>();
            for (String seat : seats) if (taken.contains(seat)) clash.add(seat);
            if (!clash.isEmpty())
                throw new IllegalArgumentException("Already booked: " + String.join(", ", clash) + ". Choose different seats.");
 
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO bookings(booking_ref,show_id,customer_name,phone,seat_number,price) VALUES(?,?,?,?,?,?)")) {
                for (String seat : seats) {
                    ps.setString(1, ref); ps.setInt(2, showId); ps.setString(3, name);
                    ps.setString(4, phone); ps.setString(5, seat); ps.setDouble(6, price);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            con.commit();
            return ref;
        } catch (SQLIntegrityConstraintViolationException | BatchUpdateException e) {
            con.rollback();
            return null;
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }
 
    
    static void printTicket(Connection con, String ref) throws SQLException {
        String sql = "SELECT b.customer_name, b.phone, b.seat_number, b.price, b.booked_at, " +
                     "m.title, m.movie_language, t.name, t.city, s.show_date, s.show_time " +
                     "FROM bookings b JOIN shows s ON s.show_id=b.show_id " +
                     "JOIN movies m ON m.movie_id=s.movie_id JOIN theaters t ON t.theater_id=s.theater_id " +
                     "WHERE b.booking_ref=? " +
                     "ORDER BY LEFT(b.seat_number,1), CAST(SUBSTRING(b.seat_number,2) AS UNSIGNED)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("No ticket found for " + ref + "."); return; }
                String customer = rs.getString(1), phone = rs.getString(2);
                String movie = rs.getString(6) + " (" + rs.getString(7) + ")";
                String theater = rs.getString(8) + ", " + rs.getString(9);
                Date date = rs.getDate(10);
                String time = rs.getTime(11).toString().substring(0, 5);
                Timestamp bookedAt = rs.getTimestamp(5);
                List<String> seats = new ArrayList<>();
                double total = 0;
                do { seats.add(rs.getString(3)); total += rs.getDouble(4); } while (rs.next());
 
                System.out.println("\n+----------------------------------------------+");
                System.out.println("|              MOVIE TICKET                    |");
                System.out.println("+----------------------------------------------+");
                System.out.println("  Ticket Ref : " + ref);
                System.out.println("  Movie      : " + movie);
                System.out.println("  Theater    : " + theater);
                System.out.println("  Date/Time  : " + date + "  " + time);
                System.out.println("  Seats      : " + String.join(", ", seats) + "  (" + seats.size() + ")");
                System.out.println("  Customer   : " + customer + "  (" + phone + ")");
                System.out.printf ("  Total Paid : %.2f%n", total);
                System.out.println("  Booked at  : " + bookedAt);
                System.out.println("+----------------------------------------------+");
            }
        }
    }
}
