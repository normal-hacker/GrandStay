-- ==========================================================
-- GRANDSTAY HOTELS & RESORTS - DATABASE SCHEMA
-- Target Database: grandstay_db
-- Target RDBMS   : MySQL / MariaDB (XAMPP default localhost:3306)
-- ==========================================================

CREATE DATABASE IF NOT EXISTS `grandstay_db` 
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `grandstay_db`;

-- Create Bookings Table if it does not already exist
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

-- Sample Initial Data (Safe Insert)
INSERT IGNORE INTO `bookings` 
(`booking_id`, `guest_name`, `email`, `phone`, `guests`, `check_in`, `check_out`, `room_type`, `booking_status`, `room_number`, `special_requests`)
VALUES
(1, 'Rahul Verma', 'rahul@example.com', '9876543210', 2, '2026-12-01', '2026-12-03', 'Standard Room', 'Pending', '101', 'Late arrival preferred'),
(2, 'Priya Nair', 'priya@example.com', '9988776655', 2, '2026-12-05', '2026-12-08', 'Deluxe Room', 'Confirmed', '205', 'Near elevator');

