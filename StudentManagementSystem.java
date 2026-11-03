package JDBC;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class StudentManagementSystem {
    static final Scanner sc = new Scanner(System.in);

    // ===================== DATABASE CONNECTION (same file) =====================
    static class DBConnection {
        // Change these 3 values to match your MySQL setup
        private static final String URL = "jdbc:mysql://localhost:3306/student_management";
        private static final String USER = "root";
        private static final String PASSWORD = "Krishna@333";

        static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }
    }
    // ===========================================================================

    public static void main(String[] args) {
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("Connected to MySQL successfully.");
            while (true) {
                System.out.println("\n=== STUDENT MANAGEMENT SYSTEM ===");
                System.out.println("1. Register student\n2. View students\n3. Update student\n4. Delete student");
                System.out.println("5. Add course\n6. View courses\n7. Enroll student\n8. View enrollments");
                System.out.println("9. Mark attendance\n10. Attendance report\n11. Enter/update marks\n12. Results");
                System.out.println("13. Search student\n0. Exit");
                int choice = readInt("Choice: ", 0, 13);
                try {
                    switch (choice) {
                        case 1: register(con); break;
                        case 2: viewStudents(con); break;
                        case 3: updateStudent(con); break;
                        case 4: deleteStudent(con); break;
                        case 5: addCourse(con); break;
                        case 6: viewCourses(con); break;
                        case 7: enroll(con); break;
                        case 8: viewEnrollments(con); break;
                        case 9: markAttendance(con); break;
                        case 10: attendanceReport(con); break;
                        case 11: enterMarks(con); break;
                        case 12: results(con); break;
                        case 13: searchStudent(con); break;
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

    static double readMarks() {
        while (true) {
            System.out.print("Marks (0-100): ");
            try {
                double n = Double.parseDouble(sc.nextLine().trim());
                if (Double.isFinite(n) && n >= 0 && n <= 100 && Math.round(n * 100) / 100.0 == n) return n;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter valid marks from 0 to 100 with at most 2 decimal places.");
        }
    }

    static String readEmail() {
        while (true) {
            String email = readText("Email: ", 120);
            if (email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) return email;
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

    static LocalDate readDate() {
        while (true) {
            String raw = readText("Date (YYYY-MM-DD): ", 10);
            try {
                LocalDate d = LocalDate.parse(raw);
                if (!d.isAfter(LocalDate.now())) return d;
            } catch (DateTimeParseException ignored) { }
            System.out.println("Enter a real date not in the future.");
        }
    }

    static boolean exists(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    static void register(Connection con) throws SQLException {
        String name = readText("Student name: ", 100);
        if (!name.matches("[\\p{L} .'-]+")) { System.out.println("Name must contain letters and spaces/punctuation only."); return; }
        int age = readInt("Age: ", 15, 100);
        String email = readEmail();
        String phone = readPhone();
        String sql = "INSERT INTO students(student_name,age,email,phone) VALUES(?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name); ps.setInt(2, age); ps.setString(3, email); ps.setString(4, phone);
            ps.executeUpdate(); System.out.println("Student registered.");
        }
    }

    static void viewStudents(Connection con) throws SQLException {
        String sql = "SELECT * FROM students ORDER BY student_id";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery())
        {
            System.out.println();
            System.out.printf("%-5s %-25s %-5s %-30s %-12s%n", "ID", "Name", "Age", "Email", "Phone");
            System.out.println("=====================================================================");
            while (rs.next()) {
                System.out.printf("%-5d %-25s %-5d %-30s %-12s%n", rs.getInt("student_id"), rs.getString("student_name"), rs.getInt("age"), rs.getString("email"), rs.getString("phone"));
                System.out.println("===================================================================");
            }
        }
    }

    static void updateStudent(Connection con) throws SQLException {
        int id = readInt("Student ID: ", 1, Integer.MAX_VALUE);
        if (!exists(con, "SELECT 1 FROM students WHERE student_id=?", id)) { System.out.println("Student not found."); return; }
        String name = readText("New name: ", 100);
        if (!name.matches("[\\p{L} .'-]+")) { System.out.println("Invalid name."); return; }
        int age = readInt("New age: ", 15, 100);
        String email = readEmail(), phone = readPhone();
        try (PreparedStatement ps = con.prepareStatement("UPDATE students SET student_name=?,age=?,email=?,phone=? WHERE student_id=?")) {
            ps.setString(1, name); ps.setInt(2, age); ps.setString(3, email); ps.setString(4, phone); ps.setInt(5, id);
            System.out.println(ps.executeUpdate() + " student updated.");
        }
    }

    static void deleteStudent(Connection con) throws SQLException {
        int id = readInt("Student ID to delete: ", 1, Integer.MAX_VALUE);
        if (!exists(con, "SELECT 1 FROM students WHERE student_id=?", id)) { System.out.println("Student not found."); return; }
        if (exists(con, "SELECT 1 FROM enrollments WHERE student_id=?", id)) {
            System.out.println("Cannot delete: student has enrollments. Keep academic history intact."); return;
        }
        if (!readText("Type YES to confirm deletion: ", 10).equals("YES")) { System.out.println("Cancelled."); return; }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM students WHERE student_id=?")) {
            ps.setInt(1, id); System.out.println(ps.executeUpdate() + " student deleted.");
        }
    }

    static void addCourse(Connection con) throws SQLException {
        String name = readText("Course name: ", 100);
        int duration = readInt("Duration in months: ", 1, 60);
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO courses(course_name,duration_months) VALUES(?,?)")) {
            ps.setString(1, name); ps.setInt(2, duration); ps.executeUpdate(); System.out.println("Course added.");
        }
    }

    static void viewCourses(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM courses ORDER BY course_id"); ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-6s %-30s %-8s%n", "ID", "Course", "Months");
            while (rs.next()) System.out.printf("%-6d %-30s %-8d%n", rs.getInt(1), rs.getString(2), rs.getInt(3));
        }
    }

    static void enroll(Connection con) throws SQLException {
        int studentId = readInt("Student ID: ", 1, Integer.MAX_VALUE);
        int courseId = readInt("Course ID: ", 1, Integer.MAX_VALUE);
        boolean oldAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            if (!exists(con, "SELECT 1 FROM students WHERE student_id=?", studentId) ||
                !exists(con, "SELECT 1 FROM courses WHERE course_id=?", courseId)) {
                System.out.println("Student or course does not exist."); con.rollback(); return;
            }
            if (existsPair(con, studentId, courseId)) {
                System.out.println("Student already enrolled in this course."); con.rollback(); return;
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO enrollments(student_id,course_id,enrollment_date) VALUES(?,?,?)")) {
                ps.setInt(1, studentId); ps.setInt(2, courseId); ps.setDate(3, Date.valueOf(LocalDate.now()));
                ps.executeUpdate();
            }
            con.commit(); System.out.println("Enrollment committed successfully.");
        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } finally { con.setAutoCommit(oldAutoCommit); }
    }

    static boolean existsPair(Connection con, int studentId, int courseId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM enrollments WHERE student_id=? AND course_id=?")) {
            ps.setInt(1, studentId); ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    static void viewEnrollments(Connection con) throws SQLException {
        String sql = "SELECT e.enrollment_id,s.student_name,c.course_name,e.enrollment_date FROM enrollments e " +
                     "JOIN students s ON s.student_id=e.student_id JOIN courses c ON c.course_id=e.course_id ORDER BY e.enrollment_id";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-6s %-25s %-25s %-12s%n", "ID", "Student", "Course", "Date");
            while (rs.next()) System.out.printf("%-6d %-25s %-25s %-12s%n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDate(4));
        }
    }

    static void markAttendance(Connection con) throws SQLException {
        int enrollmentId = readInt("Enrollment ID: ", 1, Integer.MAX_VALUE);
        if (!exists(con, "SELECT 1 FROM enrollments WHERE enrollment_id=?", enrollmentId)) { System.out.println("Enrollment not found."); return; }
        LocalDate day = readDate();
        int status = readInt("1. Present  2. Absent: ", 1, 2);
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO attendance(enrollment_id,attendance_date,status) VALUES(?,?,?)")) {
            ps.setInt(1, enrollmentId); ps.setDate(2, Date.valueOf(day)); ps.setString(3, status == 1 ? "Present" : "Absent");
            ps.executeUpdate(); System.out.println("Attendance saved.");
        }
    }

    static void attendanceReport(Connection con) throws SQLException {
        String sql = "SELECT e.enrollment_id,s.student_name,c.course_name,COUNT(a.attendance_id) total," +
                     "COALESCE(SUM(CASE WHEN a.status='Present' THEN 1 ELSE 0 END),0) present " +
                     "FROM enrollments e JOIN students s ON s.student_id=e.student_id " +
                     "JOIN courses c ON c.course_id=e.course_id LEFT JOIN attendance a ON a.enrollment_id=e.enrollment_id " +
                     "GROUP BY e.enrollment_id,s.student_name,c.course_name ORDER BY e.enrollment_id";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-5s %-22s %-22s %-8s %-8s %-8s%n", "ID", "Student", "Course", "Total", "Present", "%");
            while (rs.next()) {
                int total = rs.getInt("total"), present = rs.getInt("present");
                System.out.printf("%-5d %-22s %-22s %-8d %-8d %-8.2f%n", rs.getInt(1), rs.getString(2), rs.getString(3), total, present, total == 0 ? 0.0 : 100.0 * present / total);
            }
        }
    }

    static void enterMarks(Connection con) throws SQLException {
        int id = readInt("Enrollment ID: ", 1, Integer.MAX_VALUE);
        if (!exists(con, "SELECT 1 FROM enrollments WHERE enrollment_id=?", id)) { System.out.println("Enrollment not found."); return; }
        double score = readMarks();
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO marks(enrollment_id,marks_obtained) VALUES(?,?) " +
                "ON DUPLICATE KEY UPDATE marks_obtained=VALUES(marks_obtained)")) {
            ps.setInt(1, id); ps.setDouble(2, score); ps.executeUpdate(); System.out.println("Marks saved.");
        }
    }

    static void results(Connection con) throws SQLException {
        String sql = "SELECT s.student_id,s.student_name,c.course_name,m.marks_obtained FROM enrollments e " +
                     "JOIN students s ON s.student_id=e.student_id JOIN courses c ON c.course_id=e.course_id " +
                     "LEFT JOIN marks m ON m.enrollment_id=e.enrollment_id ORDER BY s.student_id,c.course_name";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-5s %-22s %-22s %-10s %-10s%n", "ID", "Student", "Course", "Marks", "Grade");
            while (rs.next()) {
                double mark = rs.getDouble(4);
                boolean pending = rs.wasNull();
                String grade = pending ? "Pending" : mark >= 90 ? "A+" : mark >= 80 ? "A" : mark >= 70 ? "B" : mark >= 60 ? "C" : mark >= 50 ? "D" : "F";
                System.out.printf("%-5d %-22s %-22s %-10s %-10s%n", rs.getInt(1), rs.getString(2), rs.getString(3), pending ? "Pending" : String.format("%.2f", mark), grade);
            }
        }
    }

    static void searchStudent(Connection con) throws SQLException {
        int id = readInt("Student ID: ", 1, Integer.MAX_VALUE);
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM students WHERE student_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Student not found."); return; }
                System.out.printf("ID: %d%nName: %s%nAge: %d%nEmail: %s%nPhone: %s%n", rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5));
            }
        }
    }
}