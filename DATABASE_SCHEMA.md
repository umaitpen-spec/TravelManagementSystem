# Database Schema

## Tables

### users
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `email` VARCHAR UNIQUE NOT NULL
- `password` VARCHAR NOT NULL
- `name` VARCHAR NOT NULL
- `role` VARCHAR
- `created_at` DATETIME
- `updated_at` DATETIME

### trips
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `title` VARCHAR
- `destination` VARCHAR
- `start_date` DATE
- `end_date` DATE
- `travelers` INT
- `budget` DOUBLE
- `owner_id` BIGINT FOREIGN KEY -> users(id)
- `created_at` DATETIME
- `updated_at` DATETIME

### itineraries
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `day_label` VARCHAR
- `activities_json` TEXT
- `estimated_cost` DOUBLE
- `trip_id` BIGINT FOREIGN KEY -> trips(id)
- `created_at` DATETIME
- `updated_at` DATETIME

### bookings
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `provider` VARCHAR
- `booking_type` VARCHAR
- `reference` VARCHAR
- `price` DOUBLE
- `currency` VARCHAR DEFAULT 'INR'
- `trip_id` BIGINT FOREIGN KEY -> trips(id)
- `created_at` DATETIME
- `updated_at` DATETIME

### expenses
- `id` BIGINT PRIMARY KEY AUTO_INCREMENT
- `category` VARCHAR
- `description` VARCHAR
- `amount` DOUBLE
- `currency` VARCHAR DEFAULT 'INR'
- `trip_id` BIGINT FOREIGN KEY -> trips(id)
- `created_at` DATETIME
- `updated_at` DATETIME

## Indexes
- `users.email` unique index
- `trips.owner_id` index
- `itineraries.trip_id` index
- `bookings.trip_id` index
- `expenses.trip_id` index
