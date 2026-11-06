# GrandStay Hotels & Resorts — Hotel Booking Management System

A college-level Java mini-project featuring a luxury HTML/CSS/JS frontend, a Core Java backend powered by Java's built-in `com.sun.net.httpserver.HttpServer`, and MySQL persistence via JDBC.

---

## 1. Project Architecture & Technologies

- **Frontend**: HTML5, CSS3, Vanilla JavaScript (Luxury dark & champagne gold theme, zero external JS libraries)
- **Backend**: Core Java (Java 21 / 27)
  - `com.sun.net.httpserver.HttpServer` (built-in Java HTTP server, no Tomcat / Spring Boot required)
  - Pure JDBC with `PreparedStatement` (SQL injection prevention)
  - OOP Architecture (`HotelBooking` Model, `BookingManager` DAO/Service, `GrandStayServer` Controller)
- **Database**: XAMPP MySQL / MariaDB (`grandstay_db` on `localhost:3306`)
- **Driver**: MySQL Connector/J (`mysql-connector-j-26.10.0.jar`)
- **OS Environment**: macOS (Terminal & zsh)

---

## 2. Directory Structure

```
GrandStay/
├── index.html                   # Luxury Homepage & Room Search
├── admin.html                   # Staff Admin Dashboard (KPIs, Table, Confirm/Cancel)
├── database.sql                 # Safe MySQL table creation & sample data script
├── README.md                    # Setup and viva documentation
│
├── css/
│   └── style.css                # Luxury styling (Charcoal, Obsidian & Champagne Gold)
│
├── pages/
│   ├── rooms.html               # Accommodation showcase with rates & booking links
│   ├── booking.html             # Interactive reservation form with live price calculator
│   ├── about.html               # GrandStay story, vision & core values
│   ├── amenities.html           # 5-star facilities & services
│   └── contact.html             # Contact information & message form
│
├── DBConnection.java            # JDBC connection manager to grandstay_db
├── HotelBooking.java            # Encapsulated OOP model with pricing logic
├── BookingManager.java          # JDBC CRUD operations using PreparedStatement
├── GrandStayServer.java         # Built-in Java HTTP server & REST API
├── Main.java                    # Interactive terminal console reservation interface
├── TestConnection.java          # Utility to test MySQL connectivity
└── mysql-connector-j-26.10.0.jar # MySQL JDBC Driver JAR
```

---

## 3. Step-by-Step macOS Setup Instructions

### Step 1: Start Apache and MySQL in XAMPP
1. Open **XAMPP Application Manager** (`/Applications/XAMPP/manager-osx.app` or via Applications).
2. Go to the **Manage Servers** tab.
3. Select **MySQL Database** and click **Start**.
4. (Optional) Start **Apache Web Server** if you wish to use phpMyAdmin via browser.
5. Verify both indicators turn green.

*Alternatively, from macOS Terminal:*
```bash
sudo /Applications/XAMPP/xamppfiles/xampp startmysql
```

---

### Step 2: Create or Verify the Database in phpMyAdmin
1. Open your browser and navigate to:
   ```
   http://localhost/phpmyadmin
   ```
2. In the left sidebar, click **New** and create a database named:
   ```
   grandstay_db
   ```
   Collation: `utf8mb4_general_ci`.
3. Click on `grandstay_db`, open the **SQL** tab, and run the SQL script from `database.sql`:
   ```sql
   CREATE TABLE IF NOT EXISTS `bookings` (
     `booking_id` INT(11) NOT NULL AUTO_INCREMENT,
     `guest_name` VARCHAR(100) NOT NULL,
     `email` VARCHAR(100) NOT NULL,
     `phone` VARCHAR(20) NOT NULL,
     `guests` INT(11) NOT NULL,
     `check_in` DATE NOT NULL,
     `check_out` DATE NOT NULL,
     `room_type` VARCHAR(50) NOT NULL,
     `booking_status` VARCHAR(20) DEFAULT 'Pending',
     `room_number` VARCHAR(20) DEFAULT NULL,
     `special_requests` TEXT DEFAULT NULL,
     PRIMARY KEY (`booking_id`)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
   ```
4. Verify that the table `bookings` appears in `grandstay_db`.

---

### Step 3: Configure DBConnection.java (If Needed)
The default configuration in [DBConnection.java](file:///Users/SHADOW/Desktop/GrandStay/DBConnection.java) matches XAMPP's standard setup:
```java
private static final String URL = "jdbc:mysql://localhost:3306/grandstay_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "";
```
If your local MySQL root password is non-empty, update the `PASSWORD` constant accordingly.

To test database connectivity immediately:
```bash
javac -cp .:mysql-connector-j-26.10.0.jar DBConnection.java TestConnection.java
java -cp .:mysql-connector-j-26.10.0.jar TestConnection
```
Expected output:
```
Database Connected Successfully!
```

---

### Step 4: Compile the Java Backend Files
Open Terminal, change to the project directory, and compile **only** the required project files:
```bash
cd /Users/SHADOW/Desktop/GrandStay

javac -cp .:mysql-connector-j-26.10.0.jar \
  DBConnection.java \
  HotelBooking.java \
  BookingManager.java \
  GrandStayServer.java \
  Main.java \
  TestConnection.java
```
> [!NOTE]
> Do not use `javac *.java` so you avoid compiling unused or legacy servlet files.

---

### Step 5: Run the GrandStay Web Server
Run the HTTP server using:
```bash
java -cp .:mysql-connector-j-26.10.0.jar GrandStayServer
```

Expected startup banner:
```
==========================================================
      ★ GRANDSTAY HOTELS & RESORTS SERVER STARTED ★       
==========================================================
 • Website URL      : http://localhost:8080/index.html
 • Admin Dashboard  : http://localhost:8080/admin.html
 • Booking Endpoint : http://localhost:8080/book
 • API Bookings     : http://localhost:8080/api/bookings
 • API Status Update: http://localhost:8080/api/status
 • Health Check     : http://localhost:8080/api/health
==========================================================
```

The server:
- Listens on port `8080`.
- Serves static files (`index.html`, `admin.html`, `pages/*`, `css/*`).
- Handles CORS requests so opening files via VS Code Live Server (`http://127.0.0.1:5500`) also works smoothly.

---

### Step 6: Open the Customer Website
Open your browser and navigate to:
```
http://localhost:8080/index.html
```
You can browse:
- **Home**: Hero section, quick room search bar, introduction, room cards, and amenities.
- **Rooms** (`pages/rooms.html`): Detailed categories (Standard ₹1,500, Deluxe ₹3,000, Premium Suite ₹5,000) with "Book Now ↗" links that pre-fill the booking page.
- **About** (`pages/about.html`): Heritage, values, and hospitality philosophy.
- **Amenities** (`pages/amenities.html`): 8 facilities with interactive luxury cards.
- **Contact** (`pages/contact.html`): Contact information and feedback form.
- **Booking** (`pages/booking.html`): Reservation form with dynamic price estimation.

---

### Step 7: Open the Admin Dashboard
Open a new browser tab and navigate to:
```
http://localhost:8080/admin.html
```
Features in the Admin Portal:
- **Live Health Indicator**: Verifies backend server and MySQL connection in real-time.
- **4 Metric Cards**: Total, Pending, Confirmed, and Cancelled counts.
- **Search & Filters**: Real-time search by Guest Name, Booking ID, Phone, or Room Number; filter by Status and Category.
- **Actions**:
  - Click **Confirm** to mark a reservation as Confirmed.
  - Click **Cancel** to mark a reservation as Cancelled.
  - Click **Refresh** to fetch the newest data from MySQL.

---

### Step 8: End-to-End Test Procedure

1. Go to `http://localhost:8080/pages/booking.html`.
2. Fill in the reservation form:
   - Full Name: `Aarav Mehta`
   - Email: `aarav@example.com`
   - Phone: `9876543210`
   - Guests: `2 Guests`
   - Check-in Date: Select a future date (e.g. 15th of next month)
   - Check-out Date: Select 3 days after Check-in (e.g. 18th)
   - Room Category: Select `Premium Suite (₹5,000 / night)`
   - Room Preference: `301`
   - Notice the live price card dynamically computes:
     - 3 Nights × ₹5,000 = **₹15,000**
3. Click **Submit Reservation →**.
4. An on-screen luxury receipt modal appears confirming:
   - Reference: `#GS-<booking_id>`
   - Status: `Pending`
   - Amount: `₹15,000`
5. Switch to `http://localhost:8080/admin.html`.
6. You will see `#GS-<booking_id>` listed with status `Pending`.
7. Click **Confirm**:
   - The status badge updates to green `CONFIRMED`.
   - The Confirmed KPI card increments immediately.
   - The status is updated in MySQL database via `PreparedStatement`.

---

## 4. Optional: Terminal Console Interface
To demonstrate OOP principles and terminal-based CRUD in a college viva without the browser:
```bash
java -cp .:mysql-connector-j-26.10.0.jar Main
```
Options provided:
1. New Guest Booking (prompts for dates, category, calculates price, inserts into DB)
2. View All Bookings (displays all records in terminal)
3. Update Booking Status (Confirm / Cancel)
4. View Dashboard Statistics (Total, Pending, Confirmed, Cancelled counts)
5. Start Web Server (Instructions)
6. Exit

---

## 5. College Viva Concepts & Explanations

| Topic | Implementation in GrandStay |
|---|---|
| **Classes & Encapsulation** | `HotelBooking.java` keeps all attributes (`bookingId`, `guestName`, `checkIn`, etc.) `private` and provides `public` getters/setters and encapsulation of pricing logic (`calculateTotalPrice()`). |
| **DAO Pattern / Separation of Concerns** | `BookingManager.java` separates business logic and database CRUD operations from the HTTP server routing in `GrandStayServer.java`. |
| **PreparedStatement vs Statement** | `BookingManager` uses `PreparedStatement` with parameterized placeholders (`?`). This pre-compiles the SQL query on the MySQL server, improves execution performance, and prevents SQL Injection attacks. |
| **Retrieving Auto-Increment Keys** | `ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)` enables retrieving the newly generated `booking_id` immediately upon insertion without a secondary query. |
| **Java HttpServer** | Built-in `com.sun.net.httpserver.HttpServer` avoids the complexity, heavyweight overhead, and setup of external servlet containers like Apache Tomcat or Spring Boot. |
| **CORS (Cross-Origin Resource Sharing)** | `Access-Control-Allow-Origin: *` headers are set on all API responses so that frontend pages hosted on any port (e.g. Live Server port 5500) can make `fetch()` requests to the backend on port 8080 without browser security blocks. |
| **Collections Framework** | `ArrayList<HotelBooking>` is used to hold query results in memory; `HashMap<String, Integer>` is used to aggregate dashboard metrics. |
| **Resource Management** | Java 7+ `try-with-resources` statements are used across all database calls, guaranteeing that `Connection`, `PreparedStatement`, and `ResultSet` objects are closed automatically to prevent connection leaks. |

