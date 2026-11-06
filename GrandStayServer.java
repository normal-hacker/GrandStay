import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * GrandStayServer
 * Lightweight HTTP server built using Java's standard library (com.sun.net.httpserver.HttpServer).
 * 
 * Provides:
 * - Customer Booking API (POST /book)
 * - Admin Bookings JSON API (GET /api/bookings)
 * - Booking Status Update API (POST /api/status)
 * - Health Check API (GET /api/health)
 * - Built-in Static File Server for HTML, CSS, JS, and Assets
 * 
 * Demonstrates: HTTP protocol handling, RESTful design, JSON serialization,
 * CORS support, Input validation, and Integration with BookingManager.
 */
public class GrandStayServer {

    private static final int PORT = 8080;
    private static final BookingManager bookingManager = new BookingManager();
    private static final String PROJECT_ROOT = System.getProperty("user.dir");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 1. Customer Booking Endpoint: POST /book
        server.createContext("/book", new BookingHandler());

        // 2. Admin Bookings Retrieval Endpoint: GET /api/bookings
        server.createContext("/api/bookings", new GetBookingsHandler());

        // 3. Admin Status Update Endpoint: POST /api/status
        server.createContext("/api/status", new StatusUpdateHandler());

        // 4. System Health Check Endpoint: GET /api/health
        server.createContext("/api/health", new HealthCheckHandler());

        // 5. Static Web File Server: GET / (serves index.html, admin.html, pages/*, css/*)
        server.createContext("/", new StaticFileHandler());

        // Use default executor
        server.setExecutor(null);
        server.start();

        System.out.println("==========================================================");
        System.out.println("      ★ GRANDSTAY HOTELS & RESORTS SERVER STARTED ★       ");
        System.out.println("==========================================================");
        System.out.println(" • Website URL      : http://localhost:" + PORT + "/index.html");
        System.out.println(" • Admin Dashboard  : http://localhost:" + PORT + "/admin.html");
        System.out.println(" • Booking Endpoint : http://localhost:" + PORT + "/book");
        System.out.println(" • API Bookings     : http://localhost:" + PORT + "/api/bookings");
        System.out.println(" • API Status Update: http://localhost:" + PORT + "/api/status");
        System.out.println(" • Health Check     : http://localhost:" + PORT + "/api/health");
        System.out.println("==========================================================");
        System.out.println(" Press Ctrl+C in terminal to stop server.");
    }

    // =========================================================================
    // HANDLER 1: /book (Customer Reservation Submission)
    // =========================================================================
    static class BookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, "{\"error\": \"Method not allowed. Use POST.\"}", 405);
                return;
            }

            try {
                // Read request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                String accept = exchange.getRequestHeaders().getFirst("Accept");
                boolean wantsJson = (contentType != null && contentType.contains("application/json")) ||
                                   (accept != null && accept.contains("application/json"));

                Map<String, String> params = parseRequestBody(body, contentType);

                // Extract fields
                String guestName = getParam(params, "guestName", "name", "guest_name");
                String email = getParam(params, "email");
                String phone = getParam(params, "phone", "tel");
                String guestsStr = getParam(params, "guests");
                String checkInStr = getParam(params, "checkIn", "check_in");
                String checkOutStr = getParam(params, "checkOut", "check_out");
                String roomType = getParam(params, "roomType", "room_type");
                String roomNumber = getParam(params, "roomNumber", "room_number");
                String specialRequests = getParam(params, "requests", "special_requests", "specialRequests");

                // Validation
                List<String> errors = new ArrayList<>();
                if (guestName == null || guestName.trim().length() < 2) {
                    errors.add("Full name is required (minimum 2 characters).");
                }
                if (email == null || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    errors.add("A valid email address is required (e.g. guest@example.com).");
                }
                if (phone == null || phone.replaceAll("[^0-9]", "").length() < 7) {
                    errors.add("A valid phone number with at least 7 digits is required.");
                }

                int guests = 2;
                try {
                    guests = Integer.parseInt(guestsStr);
                    if (guests < 1 || guests > 10) {
                        errors.add("Number of guests must be between 1 and 10.");
                    }
                } catch (Exception e) {
                    errors.add("Invalid number of guests specified.");
                }

                Date checkInDate = null;
                Date checkOutDate = null;
                LocalDate today = LocalDate.now();

                try {
                    checkInDate = Date.valueOf(checkInStr);
                    if (checkInDate.toLocalDate().isBefore(today)) {
                        errors.add("Check-in date cannot be in the past.");
                    }
                } catch (Exception e) {
                    errors.add("Check-in date must be a valid date format (YYYY-MM-DD).");
                }

                try {
                    checkOutDate = Date.valueOf(checkOutStr);
                    if (checkInDate != null && !checkOutDate.after(checkInDate)) {
                        errors.add("Check-out date must be after check-in date.");
                    }
                } catch (Exception e) {
                    errors.add("Check-out date must be a valid date format (YYYY-MM-DD).");
                }

                if (roomType == null || roomType.trim().isEmpty()) {
                    errors.add("Please select a room category.");
                }

                // If validation failed, respond with descriptive error
                if (!errors.isEmpty()) {
                    if (wantsJson) {
                        StringBuilder errJson = new StringBuilder("{\"success\":false,\"errors\":[");
                        for (int i = 0; i < errors.size(); i++) {
                            if (i > 0) errJson.append(",");
                            errJson.append(quote(errors.get(i)));
                        }
                        errJson.append("]}");
                        sendJsonResponse(exchange, errJson.toString(), 400);
                    } else {
                        sendHtmlResponse(exchange, renderErrorPage(errors), 400);
                    }
                    return;
                }

                // Construct model object and persist to database
                HotelBooking booking = new HotelBooking(
                    guestName.trim(),
                    email.trim(),
                    phone.trim(),
                    guests,
                    checkInDate,
                    checkOutDate,
                    roomType.trim(),
                    (roomNumber != null ? roomNumber.trim() : ""),
                    (specialRequests != null ? specialRequests.trim() : "")
                );

                int bookingId = bookingManager.addBooking(booking);

                // Respond with success
                if (wantsJson) {
                    String json = String.format(Locale.US,
                        "{\"success\":true,\"bookingId\":%d,\"guestName\":%s,\"email\":%s,\"phone\":%s,\"guests\":%d,\"checkIn\":%s,\"checkOut\":%s,\"roomType\":%s,\"roomNumber\":%s,\"nights\":%d,\"ratePerNight\":%d,\"totalPrice\":%d,\"status\":%s}",
                        bookingId,
                        quote(booking.getGuestName()),
                        quote(booking.getEmail()),
                        quote(booking.getPhone()),
                        booking.getGuests(),
                        quote(booking.getCheckIn().toString()),
                        quote(booking.getCheckOut().toString()),
                        quote(booking.getRoomType()),
                        quote(booking.getRoomNumber()),
                        booking.getNights(),
                        booking.getRatePerNight(),
                        booking.calculateTotalPrice(),
                        quote(booking.getBookingStatus())
                    );
                    sendJsonResponse(exchange, json, 200);
                } else {
                    sendHtmlResponse(exchange, renderConfirmationPage(booking), 200);
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, "{\"success\":false,\"error\":" + quote("Booking processing failed: " + e.getMessage()) + "}", 500);
            }
        }
    }

    // =========================================================================
    // HANDLER 2: /api/bookings (Retrieve All Bookings for Admin)
    // =========================================================================
    static class GetBookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, "{\"error\":\"GET method required\"}", 405);
                return;
            }

            try {
                List<HotelBooking> list = bookingManager.getAllBookings();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    HotelBooking b = list.get(i);
                    json.append("{")
                        .append("\"booking_id\":").append(b.getBookingId()).append(",")
                        .append("\"guest_name\":").append(quote(b.getGuestName())).append(",")
                        .append("\"email\":").append(quote(b.getEmail())).append(",")
                        .append("\"phone\":").append(quote(b.getPhone())).append(",")
                        .append("\"guests\":").append(b.getGuests()).append(",")
                        .append("\"check_in\":").append(quote(b.getCheckIn() != null ? b.getCheckIn().toString() : "")).append(",")
                        .append("\"check_out\":").append(quote(b.getCheckOut() != null ? b.getCheckOut().toString() : "")).append(",")
                        .append("\"room_type\":").append(quote(b.getRoomType())).append(",")
                        .append("\"booking_status\":").append(quote(b.getBookingStatus())).append(",")
                        .append("\"room_number\":").append(quote(b.getRoomNumber() != null ? b.getRoomNumber() : "")).append(",")
                        .append("\"special_requests\":").append(quote(b.getSpecialRequests() != null ? b.getSpecialRequests() : "")).append(",")
                        .append("\"nights\":").append(b.getNights()).append(",")
                        .append("\"rate_per_night\":").append(b.getRatePerNight()).append(",")
                        .append("\"total_price\":").append(b.calculateTotalPrice())
                        .append("}");
                }
                json.append("]");
                sendJsonResponse(exchange, json.toString(), 200);

            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, "{\"error\":" + quote("Failed to retrieve bookings: " + e.getMessage()) + "}", 500);
            }
        }
    }

    // =========================================================================
    // HANDLER 3: /api/status (Update Booking Status)
    // =========================================================================
    static class StatusUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, "{\"error\":\"POST method required\"}", 405);
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                Map<String, String> params = parseRequestBody(body, contentType);

                String bookingIdStr = getParam(params, "bookingId", "id", "booking_id");
                String status = getParam(params, "status", "booking_status");

                if (bookingIdStr == null || status == null) {
                    sendJsonResponse(exchange, "{\"success\":false,\"error\":\"bookingId and status are required\"}", 400);
                    return;
                }

                int bookingId;
                try {
                    bookingId = Integer.parseInt(bookingIdStr);
                } catch (NumberFormatException e) {
                    sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Invalid booking ID\"}", 400);
                    return;
                }

                if (!"Confirmed".equalsIgnoreCase(status) &&
                    !"Cancelled".equalsIgnoreCase(status) &&
                    !"Pending".equalsIgnoreCase(status)) {
                    sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Status must be Confirmed, Cancelled, or Pending\"}", 400);
                    return;
                }

                boolean updated = bookingManager.updateStatus(bookingId, status);
                if (updated) {
                    sendJsonResponse(exchange,
                        String.format("{\"success\":true,\"bookingId\":%d,\"status\":%s,\"message\":\"Booking updated successfully\"}",
                            bookingId, quote(status)),
                        200
                    );
                } else {
                    sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Booking not found with ID " + bookingId + "\"}", 404);
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, "{\"success\":false,\"error\":" + quote("Update error: " + e.getMessage()) + "}", 500);
            }
        }
    }

    // =========================================================================
    // HANDLER 4: /api/health (Health & Database Connectivity Check)
    // =========================================================================
    static class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            boolean dbOk = DBConnection.testConnection();
            String timestamp = LocalDateTime.now().toString();

            if (dbOk) {
                String response = String.format(
                    "{\"status\":\"UP\",\"database\":\"Connected\",\"service\":\"GrandStay Server\",\"timestamp\":%s}",
                    quote(timestamp)
                );
                sendJsonResponse(exchange, response, 200);
            } else {
                String response = String.format(
                    "{\"status\":\"DEGRADED\",\"database\":\"Disconnected\",\"error\":\"Check XAMPP MySQL on port 3306\",\"timestamp\":%s}",
                    quote(timestamp)
                );
                sendJsonResponse(exchange, response, 503);
            }
        }
    }

    // =========================================================================
    // HANDLER 5: Static File Handler (Serves frontend files from disk)
    // =========================================================================
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
                return;
            }

            String rawPath = exchange.getRequestURI().getPath();
            if (rawPath == null || rawPath.equals("/")) {
                rawPath = "/index.html";
            }

            // Prevent path traversal
            if (rawPath.contains("..")) {
                sendHtmlResponse(exchange, "<h1>403 Forbidden</h1>", 403);
                return;
            }

            // Resolve file relative to PROJECT_ROOT
            Path filePath = Paths.get(PROJECT_ROOT, rawPath.substring(1));
            File file = filePath.toFile();

            if (!file.exists() || file.isDirectory()) {
                // If not found, show 404 page
                String notFoundHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>404 Not Found - GrandStay</title><style>
                    body { font-family: sans-serif; background: #171714; color: #f8f6f1; text-align: center; padding: 100px 20px; }
                    h1 { color: #c8a46b; font-size: 48px; }
                    a { color: #e0c28f; text-decoration: none; border-bottom: 1px solid #c8a46b; padding-bottom: 4px; }
                    </style></head>
                    <body>
                    <h1>404 - Page Not Found</h1>
                    <p>The requested resource was not found on GrandStay server.</p>
                    <p><a href="/index.html">&larr; Return to GrandStay Home</a></p>
                    </body></html>
                    """;
                sendHtmlResponse(exchange, notFoundHtml, 404);
                return;
            }

            String mimeType = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            addCorsHeaders(exchange);

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getMimeType(String fileName) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }

    // =========================================================================
    // HELPER METHODS: Parsing, Responses & HTML Templates
    // =========================================================================

    private static Map<String, String> parseRequestBody(String body, String contentType) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        // Check if JSON
        if (contentType != null && contentType.contains("application/json")) {
            String trimmed = body.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
                String[] tokens = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String token : tokens) {
                    String[] kv = token.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replaceAll("^\"|\"$", "");
                        String val = kv[1].trim().replaceAll("^\"|\"$", "");
                        map.put(key, val);
                    }
                }
            }
            return map;
        }

        // Default: application/x-www-form-urlencoded
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length > 0) {
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                map.put(key, value);
            }
        }
        return map;
    }

    private static String getParam(Map<String, String> params, String... keys) {
        for (String k : keys) {
            if (params.containsKey(k) && params.get(k) != null && !params.get(k).trim().isEmpty()) {
                return params.get(k).trim();
            }
        }
        return null;
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
    }

    private static void sendJsonResponse(HttpExchange exchange, String json, int status) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendHtmlResponse(HttpExchange exchange, String html, int status) {
        try {
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String quote(String val) {
        if (val == null) return "null";
        return "\"" + val
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\"";
    }

    /**
     * Renders a luxury HTML confirmation page matching GrandStay's theme.
     */
    private static String renderConfirmationPage(HotelBooking b) {
        String template = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reservation Confirmed | GrandStay Hotels & Resorts</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:wght@500;600;700&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --gold: #c8a46b;
                        --gold-light: #e0c28f;
                        --dark: #171714;
                        --dark-soft: #24231f;
                        --cream: #f8f6f1;
                        --white: #ffffff;
                        --muted: #8c8982;
                    }
                    * { margin:0; padding:0; box-sizing:border-box; }
                    body {
                        font-family: 'DM Sans', sans-serif;
                        background: #11110f;
                        color: var(--cream);
                        min-height: 100vh;
                        display: flex;
                        flex-direction: column;
                    }
                    .navbar {
                        height: 85px;
                        padding: 0 8%;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        background: #171714;
                        border-bottom: 1px solid #282723;
                    }
                    .logo {
                        font-family: 'Playfair Display', serif;
                        font-size: 26px;
                        font-weight: 700;
                        color: white;
                        text-decoration: none;
                        letter-spacing: 1px;
                    }
                    .logo span { color: var(--gold); }
                    .logo small {
                        display: block;
                        font-family: 'DM Sans', sans-serif;
                        font-size: 8px;
                        letter-spacing: 3px;
                        color: #aaa;
                    }
                    .container {
                        max-width: 800px;
                        margin: 50px auto;
                        padding: 0 20px;
                        flex: 1;
                    }
                    .card {
                        background: #1c1b18;
                        border: 1px solid #2f2e29;
                        padding: 45px;
                        box-shadow: 0 25px 60px rgba(0,0,0,0.5);
                        position: relative;
                    }
                    .card::before {
                        content: '';
                        position: absolute;
                        top: 0; left: 0; right: 0;
                        height: 3px;
                        background: linear-gradient(90deg, var(--gold), var(--gold-light), var(--gold));
                    }
                    .badge-wrap {
                        text-align: center;
                        margin-bottom: 25px;
                    }
                    .badge {
                        display: inline-block;
                        background: rgba(200, 164, 107, 0.15);
                        color: var(--gold-light);
                        border: 1px solid var(--gold);
                        padding: 6px 18px;
                        font-size: 11px;
                        letter-spacing: 2px;
                        text-transform: uppercase;
                        font-weight: 600;
                    }
                    h1 {
                        font-family: 'Playfair Display', serif;
                        font-size: 34px;
                        text-align: center;
                        font-weight: 500;
                        margin-bottom: 12px;
                    }
                    h1 span { color: var(--gold); }
                    .subtitle {
                        text-align: center;
                        color: var(--muted);
                        font-size: 14px;
                        margin-bottom: 35px;
                    }
                    .summary-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        background: #141412;
                        padding: 25px;
                        border: 1px solid #272622;
                        margin-bottom: 30px;
                    }
                    .summary-item label {
                        display: block;
                        font-size: 10px;
                        letter-spacing: 2px;
                        color: var(--gold);
                        text-transform: uppercase;
                        margin-bottom: 5px;
                    }
                    .summary-item strong {
                        font-size: 16px;
                        color: var(--white);
                    }
                    .total-bar {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        background: rgba(200, 164, 107, 0.1);
                        border: 1px solid var(--gold);
                        padding: 20px 25px;
                        margin-bottom: 35px;
                    }
                    .total-bar span {
                        font-size: 13px;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                        color: var(--gold-light);
                    }
                    .total-bar strong {
                        font-family: 'Playfair Display', serif;
                        font-size: 26px;
                        color: var(--gold);
                    }
                    .actions {
                        display: flex;
                        gap: 15px;
                        justify-content: center;
                        flex-wrap: wrap;
                    }
                    .btn-primary {
                        background: var(--gold);
                        color: #171714;
                        font-weight: 700;
                        padding: 14px 28px;
                        text-decoration: none;
                        font-size: 13px;
                        transition: .3s ease;
                        display: inline-block;
                    }
                    .btn-primary:hover {
                        background: var(--gold-light);
                    }
                    .btn-secondary {
                        background: transparent;
                        color: var(--cream);
                        border: 1px solid #444;
                        padding: 14px 24px;
                        text-decoration: none;
                        font-size: 13px;
                        transition: .3s;
                        display: inline-block;
                    }
                    .btn-secondary:hover {
                        border-color: var(--gold);
                        color: var(--gold);
                    }
                    footer {
                        text-align: center;
                        padding: 25px;
                        color: #666;
                        font-size: 12px;
                        border-top: 1px solid #222;
                    }
                </style>
            </head>
            <body>
                <header class="navbar">
                    <a href="/index.html" class="logo">
                        GRAND<span>STAY</span>
                        <small>HOTELS & RESORTS</small>
                    </a>
                    <a href="/admin.html" class="btn-secondary" style="padding: 8px 16px; font-size: 11px;">Staff Portal &rarr;</a>
                </header>

                <div class="container">
                    <div class="card">
                        <div class="badge-wrap">
                            <span class="badge">Booking Confirmed &bull; Status: Pending Review</span>
                        </div>
                        <h1>Reservation <span>Received.</span></h1>
                        <p class="subtitle">Thank you for choosing GrandStay. Your reservation request has been registered.</p>

                        <div class="summary-grid">
                            <div class="summary-item">
                                <label>Reservation Reference</label>
                                <strong>#GS-{{ID}}</strong>
                            </div>
                            <div class="summary-item">
                                <label>Guest Name</label>
                                <strong>{{NAME}}</strong>
                            </div>
                            <div class="summary-item">
                                <label>Email Address</label>
                                <strong>{{EMAIL}}</strong>
                            </div>
                            <div class="summary-item">
                                <label>Phone Number</label>
                                <strong>{{PHONE}}</strong>
                            </div>
                            <div class="summary-item">
                                <label>Room Category</label>
                                <strong>{{ROOM}}</strong>
                            </div>
                            <div class="summary-item">
                                <label>Total Guests</label>
                                <strong>{{GUESTS}} Guest(s)</strong>
                            </div>
                            <div class="summary-item">
                                <label>Check-in Date</label>
                                <strong>{{CHECKIN}}</strong>
                            </div>
                            <div class="summary-item">
                                <label>Check-out Date</label>
                                <strong>{{CHECKOUT}} ({{NIGHTS}} night(s))</strong>
                            </div>
                        </div>

                        <div class="total-bar">
                            <span>Estimated Total Amount</span>
                            <strong>&euro; / &#8377;{{TOTAL}}</strong>
                        </div>

                        <div class="actions">
                            <a href="/index.html" class="btn-primary">Return to Homepage &rarr;</a>
                            <a href="/pages/booking.html" class="btn-secondary">Make Another Reservation</a>
                            <a href="javascript:window.print()" class="btn-secondary">Print Receipt &#9113;</a>
                        </div>
                    </div>
                </div>

                <footer>
                    <p>&copy; 2026 GrandStay Hotels & Resorts. All rights reserved.</p>
                </footer>
            </body>
            </html>
            """;

        String roomDisplay = escapeHtml(b.getRoomType());
        if (b.getRoomNumber() != null && !b.getRoomNumber().isBlank()) {
            roomDisplay += " (Room Pref: " + escapeHtml(b.getRoomNumber()) + ")";
        }

        return template
            .replace("{{ID}}", String.valueOf(b.getBookingId()))
            .replace("{{NAME}}", escapeHtml(b.getGuestName()))
            .replace("{{EMAIL}}", escapeHtml(b.getEmail()))
            .replace("{{PHONE}}", escapeHtml(b.getPhone()))
            .replace("{{ROOM}}", roomDisplay)
            .replace("{{GUESTS}}", String.valueOf(b.getGuests()))
            .replace("{{CHECKIN}}", b.getCheckIn() != null ? b.getCheckIn().toString() : "N/A")
            .replace("{{CHECKOUT}}", b.getCheckOut() != null ? b.getCheckOut().toString() : "N/A")
            .replace("{{NIGHTS}}", String.valueOf(b.getNights()))
            .replace("{{TOTAL}}", String.format("%,d", b.calculateTotalPrice()));
    }

    /**
     * Renders a styled error page if customer form submission had validation errors.
     */
    private static String renderErrorPage(List<String> errors) {
        StringBuilder list = new StringBuilder();
        for (String err : errors) {
            list.append("<li style='margin-bottom:8px;'>").append(escapeHtml(err)).append("</li>");
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Booking Error | GrandStay</title>
                <style>
                    body { font-family: 'DM Sans', sans-serif; background: #171714; color: #f8f6f1; padding: 60px 20px; text-align: center; }
                    .box { max-width: 600px; margin: 0 auto; background: #22211d; border: 1px solid #444; padding: 40px; }
                    h2 { color: #e07a5f; margin-bottom: 20px; }
                    ul { text-align: left; margin: 20px 0 30px 40px; color: #d4d1ca; }
                    a { background: #c8a46b; color: #171714; padding: 12px 24px; text-decoration: none; font-weight: bold; display: inline-block; }
                </style>
            </head>
            <body>
                <div class="box">
                    <h2>Reservation Submission Incomplete</h2>
                    <p>Please resolve the following issues and try again:</p>
                    <ul>""" + "\n" + list.toString() + "\n" + """
                    </ul>
                    <a href="javascript:history.back()">&larr; Return to Booking Form</a>
                </div>
            </body>
            </html>
            """;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
