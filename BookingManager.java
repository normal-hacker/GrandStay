import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BookingManager
 * Encapsulates all Database CRUD operations for Hotel Reservations using JDBC and PreparedStatement.
 * Demonstrates: JDBC PreparedStatement, Resource Management (try-with-resources),
 * Collections Framework (List, ArrayList, Map, HashMap), and Exception Handling.
 */
public class BookingManager {

    /**
     * Inserts a new booking into MySQL database.
     * Uses PreparedStatement with RETURN_GENERATED_KEYS to capture the auto-increment booking_id.
     * 
     * @param booking The HotelBooking object to save.
     * @return The generated booking_id.
     * @throws Exception On database or validation errors.
     */
    public int addBooking(HotelBooking booking) throws Exception {
        String sql = """
            INSERT INTO bookings 
            (guest_name, email, phone, guests, check_in, check_out, room_type, booking_status, room_number, special_requests)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, booking.getGuestName());
            ps.setString(2, booking.getEmail());
            ps.setString(3, booking.getPhone());
            ps.setInt(4, booking.getGuests());
            ps.setDate(5, booking.getCheckIn());
            ps.setDate(6, booking.getCheckOut());
            ps.setString(7, booking.getRoomType());
            ps.setString(8, (booking.getBookingStatus() != null) ? booking.getBookingStatus() : "Pending");
            ps.setString(9, booking.getRoomNumber());
            ps.setString(10, booking.getSpecialRequests());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        booking.setBookingId(id);
                        return id;
                    }
                }
            }
            throw new SQLException("Failed to retrieve generated booking ID.");
        }
    }

    /**
     * Overload for console quick testing or legacy code.
     */
    public int addBooking(String name, String email, String phone, int guests,
                          String checkIn, String checkOut, String roomType,
                          String roomNumber, String requests) throws Exception {
        HotelBooking booking = new HotelBooking(
            name, email, phone, guests,
            Date.valueOf(checkIn), Date.valueOf(checkOut),
            roomType, roomNumber, requests
        );
        return addBooking(booking);
    }

    /**
     * Backward-compatible simple method.
     */
    public void addBooking(String name, String room, int guests) {
        try {
            HotelBooking b = new HotelBooking(
                name, "guest@grandstay.com", "9876543210", guests,
                Date.valueOf("2026-12-01"), Date.valueOf("2026-12-03"),
                room, "", "Standard check-in"
            );
            int id = addBooking(b);
            System.out.println("Booking successfully created with ID: #" + id);
        } catch (Exception e) {
            System.err.println("Error adding booking: " + e.getMessage());
        }
    }

    /**
     * Retrieves all bookings ordered by latest first.
     * Demonstrates Java Collections (ArrayList).
     */
    public List<HotelBooking> getAllBookings() throws Exception {
        List<HotelBooking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings ORDER BY booking_id DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                HotelBooking b = new HotelBooking(
                    rs.getInt("booking_id"),
                    rs.getString("guest_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getInt("guests"),
                    rs.getDate("check_in"),
                    rs.getDate("check_out"),
                    rs.getString("room_type"),
                    rs.getString("booking_status"),
                    rs.getString("room_number"),
                    rs.getString("special_requests")
                );
                list.add(b);
            }
        }
        return list;
    }

    /**
     * Retrieves a single booking by its ID.
     */
    public HotelBooking getBookingById(int bookingId) throws Exception {
        String sql = "SELECT * FROM bookings WHERE booking_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new HotelBooking(
                        rs.getInt("booking_id"),
                        rs.getString("guest_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getInt("guests"),
                        rs.getDate("check_in"),
                        rs.getDate("check_out"),
                        rs.getString("room_type"),
                        rs.getString("booking_status"),
                        rs.getString("room_number"),
                        rs.getString("special_requests")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Updates the status of an existing booking using PreparedStatement.
     * Valid statuses: "Confirmed", "Cancelled", "Pending".
     */
    public boolean updateStatus(int bookingId, String status) throws Exception {
        if (!"Confirmed".equalsIgnoreCase(status) &&
            !"Cancelled".equalsIgnoreCase(status) &&
            !"Pending".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Allowed: Pending, Confirmed, Cancelled.");
        }

        // Normalize casing (e.g. "Confirmed", "Cancelled")
        String normalizedStatus = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();

        String sql = "UPDATE bookings SET booking_status = ? WHERE booking_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, normalizedStatus);
            ps.setInt(2, bookingId);

            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }

    /**
     * Aggregates booking statistics for admin dashboard metrics.
     */
    public Map<String, Integer> getStatistics() throws Exception {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("pending", 0);
        stats.put("confirmed", 0);
        stats.put("cancelled", 0);

        String sql = "SELECT booking_status, COUNT(*) as cnt FROM bookings GROUP BY booking_status";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            int total = 0;
            while (rs.next()) {
                String stName = rs.getString("booking_status");
                int count = rs.getInt("cnt");
                total += count;
                if (stName != null) {
                    stats.put(stName.toLowerCase(), count);
                }
            }
            stats.put("total", total);
        }
        return stats;
    }

    /**
     * Console display of all bookings (used by Main.java).
     */
    public void showBookings() {
        try {
            List<HotelBooking> list = getAllBookings();
            if (list.isEmpty()) {
                System.out.println("No reservations found in database.");
                return;
            }
            System.out.println("\n========== CURRENT GRANDSTAY RESERVATIONS (" + list.size() + ") ==========");
            for (HotelBooking b : list) {
                b.display();
            }
        } catch (Exception e) {
            System.err.println("Error fetching bookings: " + e.getMessage());
        }
    }
}
