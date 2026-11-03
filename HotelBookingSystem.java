package JDBC;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class HotelBookingSystem {
    static final Scanner sc = new Scanner(System.in);
    static final double GST_RATE = 0.12;
    static final int MAX_STAY_NIGHTS = 30;
    static final int MAX_ADVANCE_DAYS = 365;

    
    static class DBConnection {
        private static final String URL = "jdbc:mysql://localhost:3306/hoteldb";
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
                System.out.println("\n=== HOTEL ROOM BOOKING SYSTEM ===");
                System.out.println("1. Add room\n2. View rooms\n3. Register customer\n4. View customers");
                System.out.println("5. Check room availability\n6. Book room\n7. Check-in\n8. Check-out (generate bill)");
                System.out.println("9. View bill\n10. Cancel booking\n11. View bookings\n0. Exit");
                int choice = readInt("Choice: ", 0, 11);
                try {
                    switch (choice) {
                        case 1: addRoom(con); break;
                        case 2: viewRooms(con); break;
                        case 3: registerCustomer(con); break;
                        case 4: viewCustomers(con); break;
                        case 5: checkAvailability(con); break;
                        case 6: bookRoom(con); break;
                        case 7: checkIn(con); break;
                        case 8: checkOut(con); break;
                        case 9: viewBill(con); break;
                        case 10: cancelBooking(con); break;
                        case 11: viewBookings(con); break;
                        case 0: System.out.println("Goodbye!");
                        return;
                    }
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

    static double readDouble(String label, double min, double max) {
        while (true) {
            System.out.print(label);
            try {
                double value = Double.parseDouble(sc.nextLine().trim());
                if (Double.isFinite(value) && value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a number from " + min + " to " + max + ".");
        }
    }

    static String readEmail() {
        while (true) {
            String email = readText("Email: ", 120);
            if (email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) return email.toLowerCase();
            System.out.println("Invalid email format.");
        }
    }

    static String readPhone() {
        while (true) {
            String phone = readText("Phone (10 digits): ", 10);
            if (phone.matches("[6-9][0-9]{9}")) return phone;
            System.out.println("Enter a valid 10-digit Indian mobile number.");
        }
    }

    
    static LocalDate parseDate(String label) {
        while (true) {
            String raw = readText(label, 10);
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException ignored) {
                System.out.println("Enter a real date in YYYY-MM-DD format.");
            }
        }
    }

    static LocalDate readCheckIn() {
        while (true) {
            LocalDate d = parseDate("Check-in date (YYYY-MM-DD): ");
            if (d.isBefore(LocalDate.now())) System.out.println("Check-in date cannot be in the past.");
            else if (d.isAfter(LocalDate.now().plusDays(MAX_ADVANCE_DAYS)))
                System.out.println("Bookings open only " + MAX_ADVANCE_DAYS + " days in advance.");
            else return d;
        }
    }

    static LocalDate readCheckOut(LocalDate checkIn) {
        while (true) {
            LocalDate d = parseDate("Check-out date (YYYY-MM-DD): ");
            if (!d.isAfter(checkIn)) System.out.println("Check-out must be after check-in date.");
            else if (ChronoUnit.DAYS.between(checkIn, d) > MAX_STAY_NIGHTS)
                System.out.println("Maximum stay is " + MAX_STAY_NIGHTS + " nights.");
            else return d;
        }
    }

    static boolean exists(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    
    static void addRoom(Connection con) throws SQLException {
        String number = readText("Room number (3-4 digits): ", 4);
        if (!number.matches("[1-9][0-9]{2,3}")) { System.out.println("Room number must be 3 or 4 digits."); return; }
        int t = readInt("1. SINGLE  2. DOUBLE  3. SUITE: ", 1, 3);
        String type = t == 1 ? "SINGLE" : t == 2 ? "DOUBLE" : "SUITE";
        double price = readDouble("Price per night: ", 1, 100000);
        int capacity = readInt("Capacity (1-6): ", 1, 6);
        String sql = "INSERT INTO rooms(room_number,room_type,price_per_night,capacity) VALUES(?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, number); ps.setString(2, type); ps.setDouble(3, price); ps.setInt(4, capacity);
            ps.executeUpdate(); System.out.println("Room added.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Room " + number + " already exists.");
        }
    }

    static void viewRooms(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM rooms ORDER BY room_number"); ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-6s %-8s %-8s %-10s %-8s%n", "ID", "Room", "Type", "Price", "Capacity");
            while (rs.next())
                System.out.printf("%-6d %-8s %-8s %-10.2f %-8d%n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getInt(5));
        }
    }


    static void checkAvailability(Connection con) throws SQLException {
        LocalDate in = readCheckIn();
        LocalDate out = readCheckOut(in);
        int guests = readInt("Guests (1-6): ", 1, 6);
        String sql = "SELECT r.room_id,r.room_number,r.room_type,r.price_per_night,r.capacity FROM rooms r " +
                     "WHERE r.capacity>=? AND r.room_id NOT IN (" +
                     "SELECT b.room_id FROM bookings b WHERE b.status IN ('BOOKED','CHECKED_IN') " +
                     "AND b.check_in_date<? AND b.check_out_date>?) ORDER BY r.price_per_night,r.room_number";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, guests); ps.setDate(2, Date.valueOf(out)); ps.setDate(3, Date.valueOf(in));
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-6s %-8s %-8s %-10s %-8s%n", "ID", "Room", "Type", "Price", "Capacity");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-6d %-8s %-8s %-10.2f %-8d%n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getInt(5));
                }
                if (!any) System.out.println("No rooms available for these dates.");
            }
        }
    }

    
    static void registerCustomer(Connection con) throws SQLException {
        String name = readText("Customer name: ", 100);
        if (!name.matches("[\\p{L} .'-]{2,}")) { System.out.println("Name must contain letters and spaces/punctuation only (min 2 characters)."); return; }
        String email = readEmail();
        String phone = readPhone();
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO customers(customer_name,email,phone) VALUES(?,?,?)")) {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, phone);
            ps.executeUpdate(); System.out.println("Customer registered.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Email or phone is already registered.");
        }
    }
    

    static void viewCustomers(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM customers ORDER BY customer_id"); ResultSet rs = ps.executeQuery()) {
        	System.out.println();
        	System.out.println("============================================================");
            System.out.printf("%-5s %-25s %-30s %-12s%n", "ID", "Name", "Email", "Phone");
            System.out.println("------------------------------------------------------------");
            while (rs.next())
                System.out.printf("%-5d %-25s %-30s %-12s%n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
            System.out.println("==================================================================");
        }
    }

    
    
    static void bookRoom(Connection con) throws SQLException {
        int customerId = readInt("Customer ID: ", 1, Integer.MAX_VALUE);
        int roomId = readInt("Room ID: ", 1, Integer.MAX_VALUE);
        LocalDate in = readCheckIn();
        LocalDate out = readCheckOut(in);
        int guests = readInt("Guests (1-6): ", 1, 6);
        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            double price;
            int capacity;
            try (PreparedStatement ps = con.prepareStatement("SELECT price_per_night,capacity FROM rooms WHERE room_id=? FOR UPDATE")) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Room not found."); con.rollback(); return; }
                    price = rs.getDouble(1); capacity = rs.getInt(2);
                }
            }
            if (!exists(con, "SELECT 1 FROM customers WHERE customer_id=?", customerId)) {
                System.out.println("Customer not found."); con.rollback(); return;
            }
            if (guests > capacity) {
                System.out.println("Room capacity is " + capacity + " guest(s)."); con.rollback(); return;
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM bookings WHERE room_id=? AND status IN ('BOOKED','CHECKED_IN') " +
                    "AND check_in_date<? AND check_out_date>?")) {
                ps.setInt(1, roomId); ps.setDate(2, Date.valueOf(out)); ps.setDate(3, Date.valueOf(in));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { System.out.println("Room is not available for these dates."); con.rollback(); return; }
                }
            }
            long nights = ChronoUnit.DAYS.between(in, out);
            double estimate = totalWithGst(nights, price);
            int bookingId;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO bookings(customer_id,room_id,check_in_date,check_out_date,guests,total_amount) VALUES(?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId); ps.setInt(2, roomId);
                ps.setDate(3, Date.valueOf(in)); ps.setDate(4, Date.valueOf(out));
                ps.setInt(5, guests); ps.setDouble(6, estimate);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); bookingId = keys.getInt(1); }
            }
            con.commit();
            System.out.println("Booking committed successfully. Booking ID = " + bookingId);
            System.out.printf("Estimated total for %d night(s) incl. GST: %.2f%n", nights, estimate);
        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } finally { con.setAutoCommit(oldAutoCommit); }
    }

    
    static void checkIn(Connection con) throws SQLException {
        int id = readInt("Booking ID: ", 1, Integer.MAX_VALUE);
        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            String status;
            LocalDate in, out;
            try (PreparedStatement ps = con.prepareStatement("SELECT status,check_in_date,check_out_date FROM bookings WHERE booking_id=? FOR UPDATE")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Booking not found."); con.rollback(); return; }
                    status = rs.getString(1); in = rs.getDate(2).toLocalDate(); out = rs.getDate(3).toLocalDate();
                }
            }
            LocalDate today = LocalDate.now();
            if (!status.equals("BOOKED")) { System.out.println("Cannot check in. Booking status is " + status + "."); con.rollback(); return; }
            if (today.isBefore(in)) { System.out.println("Too early. Check-in opens on " + in + "."); con.rollback(); return; }
            if (!today.isBefore(out)) { System.out.println("Booking expired (check-out date was " + out + ")."); con.rollback(); return; }
            try (PreparedStatement ps = con.prepareStatement("UPDATE bookings SET status='CHECKED_IN',actual_check_in=? WHERE booking_id=?")) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now())); ps.setInt(2, id);
                ps.executeUpdate();
            }
            con.commit(); System.out.println("Check-in committed successfully.");
        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } finally { con.setAutoCommit(oldAutoCommit); }
    }

    
    static void checkOut(Connection con) throws SQLException {
        int id = readInt("Booking ID: ", 1, Integer.MAX_VALUE);
        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            String sql = "SELECT b.status,b.check_in_date,b.check_out_date,r.price_per_night,r.room_number,r.room_type,c.customer_name " +
                         "FROM bookings b JOIN rooms r ON r.room_id=b.room_id JOIN customers c ON c.customer_id=b.customer_id " +
                         "WHERE b.booking_id=? FOR UPDATE";
            String status, room, type, guest;
            LocalDate in, plannedOut;
            double price;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Booking not found."); con.rollback(); return; }
                    status = rs.getString(1); in = rs.getDate(2).toLocalDate(); plannedOut = rs.getDate(3).toLocalDate();
                    price = rs.getDouble(4); room = rs.getString(5); type = rs.getString(6); guest = rs.getString(7);
                }
            }
            if (!status.equals("CHECKED_IN")) { System.out.println("Cannot check out. Booking status is " + status + "."); con.rollback(); return; }

            LocalDateTime now = LocalDateTime.now();
            long nights = billedNights(in, plannedOut, now.toLocalDate());
            double total = totalWithGst(nights, price);
            try (PreparedStatement ps = con.prepareStatement("UPDATE bookings SET status='CHECKED_OUT',actual_check_out=?,total_amount=? WHERE booking_id=?")) {
                ps.setTimestamp(1, Timestamp.valueOf(now)); ps.setDouble(2, total); ps.setInt(3, id);
                ps.executeUpdate();
            }
            con.commit();
            System.out.println("Check-out committed successfully.");
            printBill(id, guest, room, type, price, in, plannedOut, now.toLocalDate());
        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } finally { con.setAutoCommit(oldAutoCommit); }
    }

    static void viewBill(Connection con) throws SQLException {
        int id = readInt("Booking ID: ", 1, Integer.MAX_VALUE);
        String sql = "SELECT c.customer_name,r.room_number,r.room_type,r.price_per_night,b.check_in_date,b.check_out_date," +
                     "b.actual_check_out,b.status FROM bookings b JOIN rooms r ON r.room_id=b.room_id " +
                     "JOIN customers c ON c.customer_id=b.customer_id WHERE b.booking_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Booking not found."); return; }
                if (!rs.getString("status").equals("CHECKED_OUT")) {
                    System.out.println("Bill is generated only after check-out. Status: " + rs.getString("status")); return;
                }
                printBill(id, rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4),
                          rs.getDate(5).toLocalDate(), rs.getDate(6).toLocalDate(),
                          rs.getTimestamp(7).toLocalDateTime().toLocalDate());
            }
        }
    }

    
    static long billedNights(LocalDate checkIn, LocalDate plannedOut, LocalDate actualOut) {
        LocalDate end = actualOut.isAfter(plannedOut) ? actualOut : plannedOut;
        return Math.max(1, ChronoUnit.DAYS.between(checkIn, end));
    }

    static double totalWithGst(long nights, double price) {
        return Math.round(nights * price * (1 + GST_RATE) * 100.0) / 100.0;
    }

    static void printBill(int id, String guest, String room, String type, double price,
                          LocalDate in, LocalDate plannedOut, LocalDate actualOut) {
        long nights = billedNights(in, plannedOut, actualOut);
        long extra = Math.max(0, ChronoUnit.DAYS.between(plannedOut, actualOut));
        double subtotal = nights * price;
        double gst = subtotal * GST_RATE;
        System.out.println("\n========== HOTEL BILL ==========");
        System.out.println("Booking ID   : " + id);
        System.out.println("Guest        : " + guest);
        System.out.println("Room         : " + room + " (" + type + ")");
        System.out.println("Check-in     : " + in);
        System.out.println("Check-out    : " + actualOut + " (planned " + plannedOut + ")");
        System.out.printf("Rate/night   : %.2f%n", price);
        System.out.println("Nights billed: " + nights + (extra > 0 ? " (includes " + extra + " extra night(s))" : ""));
        System.out.printf("Subtotal     : %.2f%n", subtotal);
        System.out.printf("GST (12%%)    : %.2f%n", gst);
        System.out.printf("TOTAL        : %.2f%n", subtotal + gst);
        System.out.println("================================");
    }

    
    static void cancelBooking(Connection con) throws SQLException {
        int id = readInt("Booking ID to cancel: ", 1, Integer.MAX_VALUE);
        if (!exists(con, "SELECT 1 FROM bookings WHERE booking_id=?", id)) { System.out.println("Booking not found."); return; }
        if (!readText("Type YES to confirm cancellation: ", 10).equals("YES")) { System.out.println("Cancelled."); return; }
        try (PreparedStatement ps = con.prepareStatement("UPDATE bookings SET status='CANCELLED' WHERE booking_id=? AND status='BOOKED'")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows == 1 ? "Booking cancelled." : "Only bookings in BOOKED status can be cancelled.");
        }
    }

    static void viewBookings(Connection con) throws SQLException {
        String sql = "SELECT b.booking_id,c.customer_name,r.room_number,b.check_in_date,b.check_out_date,b.guests,b.status,b.total_amount " +
                     "FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_id=b.room_id " +
                     "ORDER BY b.booking_id";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-5s %-20s %-6s %-12s %-12s %-6s %-12s %-10s%n", "ID", "Guest", "Room", "Check-in", "Check-out", "Guests", "Status", "Amount");
            while (rs.next())
                System.out.printf("%-5d %-20s %-6s %-12s %-12s %-6d %-12s %-10.2f%n", rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getDate(4), rs.getDate(5), rs.getInt(6), rs.getString(7), rs.getDouble(8));
        }
    }
}
