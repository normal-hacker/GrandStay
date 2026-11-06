import java.sql.Date;
import java.util.Map;
import java.util.Scanner;

/**
 * Main
 * Console-based Terminal Interface for the GrandStay Hotel Management System.
 * Demonstrates: OOP principles, Scanner input handling, Switch-case menus,
 * and integration with the BookingManager business logic layer.
 */
public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        BookingManager manager = new BookingManager();

        System.out.println("==================================================");
        System.out.println("     WELCOME TO GRANDSTAY HOTELS & RESORTS        ");
        System.out.println("       Terminal Reservation & Admin Console       ");
        System.out.println("==================================================");

        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. New Guest Booking");
            System.out.println("2. View All Bookings");
            System.out.println("3. Update Booking Status (Confirm / Cancel)");
            System.out.println("4. View Dashboard Statistics");
            System.out.println("5. Start Web Server (Info)");
            System.out.println("6. Exit");
            System.out.print("Enter Choice (1-6): ");

            String input = sc.nextLine().trim();
            if (input.isEmpty()) continue;

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number between 1 and 6.");
                continue;
            }

            switch (choice) {
                case 1 -> {
                    System.out.println("\n--- NEW RESERVATION ENTRY ---");
                    try {
                        System.out.print("Guest Full Name: ");
                        String name = sc.nextLine().trim();
                        if (name.isEmpty()) {
                            System.out.println("Guest name cannot be empty.");
                            break;
                        }

                        System.out.print("Email Address: ");
                        String email = sc.nextLine().trim();

                        System.out.print("Phone Number: ");
                        String phone = sc.nextLine().trim();

                        System.out.print("Number of Guests (1-4): ");
                        int guests = Integer.parseInt(sc.nextLine().trim());

                        System.out.println("\nAvailable Room Categories:");
                        System.out.println("  1. Standard Room  (₹1,500 / night)");
                        System.out.println("  2. Deluxe Room    (₹3,000 / night)");
                        System.out.println("  3. Premium Suite  (₹5,000 / night)");
                        System.out.print("Choose Category (1, 2, or 3): ");
                        String catChoice = sc.nextLine().trim();
                        String roomType = switch (catChoice) {
                            case "1" -> "Standard";
                            case "2" -> "Deluxe";
                            case "3" -> "Premium";
                            default -> "Standard";
                        };

                        System.out.print("Check-in Date (YYYY-MM-DD): ");
                        String checkInStr = sc.nextLine().trim();

                        System.out.print("Check-out Date (YYYY-MM-DD): ");
                        String checkOutStr = sc.nextLine().trim();

                        Date checkIn = Date.valueOf(checkInStr);
                        Date checkOut = Date.valueOf(checkOutStr);

                        if (!checkOut.after(checkIn)) {
                            System.out.println("Check-out date must be after check-in date!");
                            break;
                        }

                        System.out.print("Room Number Preference (optional): ");
                        String roomNumber = sc.nextLine().trim();

                        System.out.print("Special Requests (optional): ");
                        String requests = sc.nextLine().trim();

                        HotelBooking booking = new HotelBooking(
                            name, email, phone, guests,
                            checkIn, checkOut, roomType,
                            roomNumber, requests
                        );

                        int generatedId = manager.addBooking(booking);
                        System.out.println("\nSUCCESS: Reservation Confirmed!");
                        System.out.println("Generated Booking ID: #" + generatedId);
                        System.out.println("Total Nights        : " + booking.getNights());
                        System.out.println("Estimated Total Cost: ₹" + booking.calculateTotalPrice());
                        System.out.println("Initial Status      : Pending");

                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid format: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Error saving booking: " + e.getMessage());
                    }
                }

                case 2 -> {
                    manager.showBookings();
                }

                case 3 -> {
                    System.out.print("\nEnter Booking ID to update: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        HotelBooking existing = manager.getBookingById(id);
                        if (existing == null) {
                            System.out.println("No booking found with ID #" + id);
                            break;
                        }
                        System.out.println("Current status of #" + id + " (" + existing.getGuestName() + "): " + existing.getBookingStatus());
                        System.out.print("Enter New Status (Confirmed / Cancelled / Pending): ");
                        String newStatus = sc.nextLine().trim();

                        boolean ok = manager.updateStatus(id, newStatus);
                        if (ok) {
                            System.out.println("SUCCESS: Booking #" + id + " updated to " + newStatus);
                        } else {
                            System.out.println("Failed to update status.");
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }

                case 4 -> {
                    try {
                        Map<String, Integer> stats = manager.getStatistics();
                        System.out.println("\n--- GRANDSTAY DASHBOARD OVERVIEW ---");
                        System.out.println("Total Bookings     : " + stats.getOrDefault("total", 0));
                        System.out.println("Pending Bookings   : " + stats.getOrDefault("pending", 0));
                        System.out.println("Confirmed Bookings : " + stats.getOrDefault("confirmed", 0));
                        System.out.println("Cancelled Bookings : " + stats.getOrDefault("cancelled", 0));
                    } catch (Exception e) {
                        System.out.println("Error fetching statistics: " + e.getMessage());
                    }
                }

                case 5 -> {
                    System.out.println("\n--- HOW TO RUN THE WEB SERVER ---");
                    System.out.println("Execute: java -cp .:mysql-connector-j-26.10.0.jar GrandStayServer");
                    System.out.println("Website URL  : http://localhost:8080/index.html");
                    System.out.println("Admin Portal : http://localhost:8080/admin.html");
                }

                case 6 -> {
                    System.out.println("\nThank you for choosing GrandStay Hotels & Resorts. Goodbye!");
                    sc.close();
                    return;
                }

                default -> System.out.println("Invalid selection! Please choose between 1 and 6.");
            }
        }
    }
}
