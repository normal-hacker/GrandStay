import java.sql.Date;
import java.time.temporal.ChronoUnit;

/**
 * HotelBooking Model Class
 * Represents a customer reservation in the GrandStay Hotel Management System.
 * Demonstrates: Encapsulation, OOP design, getters/setters, and business logic.
 */
public class HotelBooking {

    // Encapsulated fields (Private members with public getters/setters)
    private int bookingId;
    private String guestName;
    private String email;
    private String phone;
    private int guests;
    private Date checkIn;
    private Date checkOut;
    private String roomType;
    private String bookingStatus;
    private String roomNumber;
    private String specialRequests;

    // Default Constructor
    public HotelBooking() {
        this.bookingStatus = "Pending";
    }

    // Convenience constructor for backward compatibility with console app
    public HotelBooking(int bookingId, String guestName, String roomType, int guests) {
        this.bookingId = bookingId;
        this.guestName = guestName;
        this.roomType = roomType;
        this.guests = guests;
        this.bookingStatus = "Pending";
    }

    // Full Parameterized Constructor
    public HotelBooking(int bookingId, String guestName, String email, String phone,
                        int guests, Date checkIn, Date checkOut, String roomType,
                        String bookingStatus, String roomNumber, String specialRequests) {
        this.bookingId = bookingId;
        this.guestName = guestName;
        this.email = email;
        this.phone = phone;
        this.guests = guests;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.roomType = roomType;
        this.bookingStatus = (bookingStatus != null && !bookingStatus.isBlank()) ? bookingStatus : "Pending";
        this.roomNumber = roomNumber;
        this.specialRequests = specialRequests;
    }

    // Constructor without bookingId (used before insertion into database)
    public HotelBooking(String guestName, String email, String phone,
                        int guests, Date checkIn, Date checkOut, String roomType,
                        String roomNumber, String specialRequests) {
        this(0, guestName, email, phone, guests, checkIn, checkOut, roomType, "Pending", roomNumber, specialRequests);
    }

    // Getters and Setters
    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public Date getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(Date checkIn) {
        this.checkIn = checkIn;
    }

    public Date getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(Date checkOut) {
        this.checkOut = checkOut;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    // --- Business Logic Methods ---

    /**
     * Calculates the rate per night according to room category:
     * - Standard Room: ₹1,500
     * - Deluxe Room: ₹3,000
     * - Premium Suite: ₹5,000
     */
    public int getRatePerNight() {
        if (roomType == null) return 1500;
        String type = roomType.toLowerCase().trim();
        if (type.contains("premium")) {
            return 5000;
        } else if (type.contains("deluxe")) {
            return 3000;
        } else {
            return 1500;
        }
    }

    /**
     * Calculates total number of nights between checkIn and checkOut.
     * Minimum 1 night.
     */
    public long getNights() {
        if (checkIn == null || checkOut == null) return 1;
        long days = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        return days > 0 ? days : 1;
    }

    /**
     * Calculates estimated total price in INR.
     */
    public long calculateTotalPrice() {
        return getNights() * getRatePerNight();
    }

    // Console output for terminal display
    public void display() {
        System.out.println("==========================================");
        System.out.println("Booking ID     : #" + bookingId);
        System.out.println("Guest Name     : " + guestName);
        System.out.println("Email / Phone  : " + (email != null ? email : "N/A") + " | " + (phone != null ? phone : "N/A"));
        System.out.println("Room Category  : " + roomType + (roomNumber != null && !roomNumber.isBlank() ? " (Pref: " + roomNumber + ")" : ""));
        System.out.println("Guests         : " + guests);
        System.out.println("Dates          : " + (checkIn != null ? checkIn : "N/A") + " to " + (checkOut != null ? checkOut : "N/A") + " (" + getNights() + " night(s))");
        System.out.println("Est. Amount    : ₹" + calculateTotalPrice());
        System.out.println("Status         : " + bookingStatus);
        if (specialRequests != null && !specialRequests.isBlank()) {
            System.out.println("Special Request: " + specialRequests);
        }
        System.out.println("==========================================");
    }
}
