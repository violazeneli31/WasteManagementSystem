-- =====================================================================
--  Waste Management System - database schema
--  Built from the project's ER diagram and class diagram.
--  Works on MySQL 8.0.16+ and MariaDB 10.4+ (XAMPP).
--
--  Run:   mysql -u root -p < schema.sql
--  or in phpMyAdmin: Import -> choose this file -> Go
-- =====================================================================

DROP DATABASE IF EXISTS waste_management;
CREATE DATABASE waste_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE waste_management;

-- ---------------------------------------------------------------------
--  1. USERS (User, Citizen, Institution)
--     User is the general entity; Citizen and Institution are its
--     specializations ("is" relationships in the ERD).
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone_number  VARCHAR(20),
    address       VARCHAR(255),
    points        INT UNSIGNED NOT NULL DEFAULT 0,
    user_type     ENUM('CITIZEN', 'INSTITUTION') NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE citizens (
    user_id INT UNSIGNED PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    surname VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE institutions (
    user_id INT UNSIGNED PRIMARY KEY,
    nipt    CHAR(10) NOT NULL UNIQUE,          -- Albanian tax number, e.g. K12345678A
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_nipt_length CHECK (CHAR_LENGTH(nipt) = 10)
);

-- ---------------------------------------------------------------------
--  2. EMPLOYEES (Employee and its four roles)
--     supervisor_id implements "Company Manager supervises ..." (1..*)
-- ---------------------------------------------------------------------
CREATE TABLE employees (
    emp_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    surname       VARCHAR(50)  NOT NULL,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone_number  VARCHAR(20),
    wage          DECIMAL(10, 2) NOT NULL,
    role          ENUM('MANAGER', 'GARBAGE_COLLECTOR', 'FINANCE_OFFICER', 'RECYCLING_OPERATOR') NOT NULL,
    supervisor_id INT UNSIGNED NULL,
    FOREIGN KEY (supervisor_id) REFERENCES employees (emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_wage CHECK (wage >= 0)
);

CREATE TABLE garbage_collection_employees (
    emp_id     INT UNSIGNED PRIMARY KEY,
    shift_time VARCHAR(20) NOT NULL,            -- e.g. 06:00-14:00
    FOREIGN KEY (emp_id) REFERENCES employees (emp_id) ON DELETE CASCADE
);

CREATE TABLE recycling_facilities (
    facility_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL
);

-- "Recycling Facility has 1..* Recycling Facility Operators"
CREATE TABLE recycling_facility_operators (
    emp_id      INT UNSIGNED PRIMARY KEY,
    facility_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (emp_id) REFERENCES employees (emp_id) ON DELETE CASCADE,
    FOREIGN KEY (facility_id) REFERENCES recycling_facilities (facility_id)
);

-- Days an employee cannot work (UC_02/UC_05: the manager excludes them)
CREATE TABLE employee_availability (
    emp_id       INT UNSIGNED NOT NULL,
    day          DATE NOT NULL,
    availability ENUM('AVAILABLE', 'ON_LEAVE', 'SICK', 'UNAVAILABLE') NOT NULL,
    PRIMARY KEY (emp_id, day),
    FOREIGN KEY (emp_id) REFERENCES employees (emp_id) ON DELETE CASCADE
);

CREATE TABLE attendance (
    emp_id INT UNSIGNED NOT NULL,
    day    DATE NOT NULL,
    PRIMARY KEY (emp_id, day),
    FOREIGN KEY (emp_id) REFERENCES employees (emp_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
--  3. COLLECTION (UC_01 - UC_05)
-- ---------------------------------------------------------------------
CREATE TABLE vehicles (
    vehicle_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    plate_number      VARCHAR(15) NOT NULL UNIQUE,
    fuel_level        DECIMAL(5, 2) NOT NULL DEFAULT 100,   -- percent
    current_latitude  DECIMAL(9, 6),
    current_longitude DECIMAL(9, 6),
    CONSTRAINT chk_fuel CHECK (fuel_level BETWEEN 0 AND 100)
);

-- "Vehicle is assigned to Garbage Collection Employees"
CREATE TABLE vehicle_assignments (
    vehicle_id INT UNSIGNED NOT NULL,
    emp_id     INT UNSIGNED NOT NULL,
    PRIMARY KEY (vehicle_id, emp_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (emp_id) REFERENCES garbage_collection_employees (emp_id) ON DELETE CASCADE
);

-- UC_01 Working Zone Map: "Company Manager configures Working Zone Map"
CREATE TABLE working_zone_maps (
    map_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    zone_name     VARCHAR(100) NOT NULL UNIQUE,
    configured_by INT UNSIGNED NOT NULL,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (configured_by) REFERENCES employees (emp_id)
);

-- "Working Zone Map includes Coordinates". The UNIQUE key stops the same
-- point being added twice to a map (UC_01 duplicate check).
CREATE TABLE zone_locations (
    location_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    map_id        INT UNSIGNED NOT NULL,
    name          VARCHAR(100),
    latitude      DECIMAL(9, 6) NOT NULL,
    longitude     DECIMAL(9, 6) NOT NULL,
    location_type ENUM('PICKUP_POINT', 'INSTITUTION', 'RECYCLING_FACILITY') NOT NULL DEFAULT 'PICKUP_POINT',
    FOREIGN KEY (map_id) REFERENCES working_zone_maps (map_id) ON DELETE CASCADE,
    UNIQUE KEY uq_map_point (map_id, latitude, longitude),
    CONSTRAINT chk_latitude  CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT chk_longitude CHECK (longitude BETWEEN -180 AND 180)
);

-- UC_02 input: daily waste per area (Statistics)
CREATE TABLE area_waste_statistics (
    area        VARCHAR(100) NOT NULL,
    record_date DATE NOT NULL,
    waste_kg    DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (area, record_date),
    CONSTRAINT chk_waste_kg CHECK (waste_kg >= 0)
);

-- UC_02/UC_03 Schedule: "Schedule Planner schedules Schedule", "Company Manager chooses Schedule"
CREATE TABLE schedules (
    schedule_number INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_id     INT UNSIGNED NULL,          -- NULL = not assigned yet (UC_05)
    vehicle_id      INT UNSIGNED NULL,
    schedule_date   DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    area            VARCHAR(100),
    created_by      INT UNSIGNED NOT NULL,      -- the company manager
    FOREIGN KEY (employee_id) REFERENCES garbage_collection_employees (emp_id) ON DELETE SET NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES employees (emp_id),
    CONSTRAINT chk_schedule_times CHECK (start_time < end_time)
);

-- UC_04 Route: "Vehicle follows Route", "Route follows Coordinates"
CREATE TABLE routes (
    route_id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    driver_id         INT UNSIGNED NOT NULL,
    vehicle_id        INT UNSIGNED NULL,
    schedule_number   INT UNSIGNED NULL,
    route_date        DATE NOT NULL,
    strategy          ENUM('NORMAL_ROUTINE', 'WEATHER_BASED', 'SPECIAL_PICKUPS') NOT NULL,
    total_distance_km DECIMAL(8, 2),
    FOREIGN KEY (driver_id) REFERENCES garbage_collection_employees (emp_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id) ON DELETE SET NULL,
    FOREIGN KEY (schedule_number) REFERENCES schedules (schedule_number) ON DELETE SET NULL
);

CREATE TABLE route_stops (
    route_id   INT UNSIGNED NOT NULL,
    stop_order SMALLINT UNSIGNED NOT NULL,       -- 0 = depot
    latitude   DECIMAL(9, 6) NOT NULL,
    longitude  DECIMAL(9, 6) NOT NULL,
    PRIMARY KEY (route_id, stop_order),
    FOREIGN KEY (route_id) REFERENCES routes (route_id) ON DELETE CASCADE
);

-- "User requests Pickup" (routine or special, UC_23)
CREATE TABLE pickups (
    request_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     INT UNSIGNED NOT NULL,
    status      ENUM('REQUESTED', 'SCHEDULED', 'COLLECTED', 'CANCELLED') NOT NULL DEFAULT 'REQUESTED',
    waste_type  ENUM('PAPER', 'METAL_AND_GLASS', 'ORGANIC_WASTE', 'PLASTIC', 'HAZARDOUS') NOT NULL,
    pickup_date DATE NOT NULL,
    location    VARCHAR(255) NOT NULL,
    is_special  BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
--  4. NOTIFICATIONS (UC_05, UC_21)
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    notification_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type              ENUM('USER_INFORMATIVE', 'EMPLOYEE_COLLECTION') NOT NULL,
    category          ENUM('GENERAL', 'SCHEDULE_UPDATE', 'REWARD', 'EVENT_REMINDER', 'BIN_ALERT', 'WEATHER_ALERT')
                      NOT NULL DEFAULT 'GENERAL',
    subject           VARCHAR(150) NOT NULL,
    description       TEXT NOT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sender_id         INT UNSIGNED NULL,          -- "Company Manager sends Notification"
    recipient_user_id INT UNSIGNED NULL,          -- for USER_INFORMATIVE (NULL + NULL = broadcast)
    recipient_emp_id  INT UNSIGNED NULL,          -- for EMPLOYEE_COLLECTION
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (sender_id) REFERENCES employees (emp_id) ON DELETE SET NULL,
    FOREIGN KEY (recipient_user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_emp_id) REFERENCES employees (emp_id) ON DELETE CASCADE,
    CONSTRAINT chk_one_recipient CHECK (recipient_user_id IS NULL OR recipient_emp_id IS NULL)
);

-- FR_21: which kinds of notifications each user wants
CREATE TABLE user_notification_preferences (
    user_id  INT UNSIGNED NOT NULL,
    category ENUM('GENERAL', 'SCHEDULE_UPDATE', 'REWARD', 'EVENT_REMINDER', 'BIN_ALERT', 'WEATHER_ALERT') NOT NULL,
    enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (user_id, category),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
--  5. SMART BINS AND WASTE (UC_08 - UC_14)
--     Smart_Bin "is a" Smart_Container; Smart_Container "has" a Barcode.
-- ---------------------------------------------------------------------
CREATE TABLE smart_containers (
    container_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    location           VARCHAR(255) NOT NULL,
    capacity_kg        INT UNSIGNED NOT NULL,
    current_fill_level INT UNSIGNED NOT NULL DEFAULT 0,
    barcode_serial     BIGINT UNSIGNED NOT NULL UNIQUE,
    container_kind     ENUM('SMART_BIN', 'DONATION_CONTAINER') NOT NULL,
    CONSTRAINT chk_capacity CHECK (capacity_kg > 0),
    CONSTRAINT chk_fill CHECK (current_fill_level <= capacity_kg)
);

CREATE TABLE smart_bins (
    container_id    INT UNSIGNED PRIMARY KEY,
    waste_type      ENUM('PAPER', 'METAL_AND_GLASS', 'ORGANIC_WASTE', 'PLASTIC', 'HAZARDOUS') NOT NULL,
    status          ENUM('ACTIVE', 'WARNING', 'FULL') NOT NULL DEFAULT 'ACTIVE',
    current_user_id INT UNSIGNED NULL,
    FOREIGN KEY (container_id) REFERENCES smart_containers (container_id) ON DELETE CASCADE,
    FOREIGN KEY (current_user_id) REFERENCES users (user_id) ON DELETE SET NULL
);

-- "Smart Bin contains Waste": every bag thrown in
CREATE TABLE waste_disposals (
    waste_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bin_id          INT UNSIGNED NOT NULL,
    user_id         INT UNSIGNED NOT NULL,
    waste_type      ENUM('PAPER', 'METAL_AND_GLASS', 'ORGANIC_WASTE', 'PLASTIC', 'HAZARDOUS') NOT NULL,
    weight_kg       DECIMAL(6, 2) NOT NULL,
    recycling_stage ENUM('COLLECTED', 'SORTED', 'PROCESSED', 'RECYCLED') NOT NULL DEFAULT 'COLLECTED',
    points_earned   INT UNSIGNED NOT NULL DEFAULT 0,
    disposed_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bin_id) REFERENCES smart_bins (container_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_weight CHECK (weight_kg > 0)
);

-- "Smart Bin generates Waste Report", "Company Manager receives Waste Report"
CREATE TABLE waste_reports (
    waste_report_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bin_id          INT UNSIGNED NOT NULL,
    subject         VARCHAR(150) NOT NULL,
    description     TEXT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bin_id) REFERENCES smart_bins (container_id) ON DELETE CASCADE
);

-- Company Manager oversees Inventory: smart bins in stock per type
CREATE TABLE inventory (
    waste_type    ENUM('PAPER', 'METAL_AND_GLASS', 'ORGANIC_WASTE', 'PLASTIC', 'HAZARDOUS') PRIMARY KEY,
    bins_in_stock INT UNSIGNED NOT NULL DEFAULT 0
);

-- Bin_Placement_Map: where new bins go (UC_11)
CREATE TABLE bin_placements (
    placement_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    latitude     DECIMAL(9, 6) NOT NULL,
    longitude    DECIMAL(9, 6) NOT NULL,
    waste_type   ENUM('PAPER', 'METAL_AND_GLASS', 'ORGANIC_WASTE', 'PLASTIC', 'HAZARDOUS') NOT NULL,
    placed       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uq_placement (latitude, longitude, waste_type)
);

-- ---------------------------------------------------------------------
--  6. REPORTS AND FEEDBACK (UC_20)
-- ---------------------------------------------------------------------
CREATE TABLE reports (
    report_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     INT UNSIGNED NULL,               -- "User submits Report"
    employee_id INT UNSIGNED NULL,               -- or an employee sends it
    subject     VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status      ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'OPEN',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_by  INT UNSIGNED NULL,               -- "Finance Officer manages Report"
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES employees (emp_id) ON DELETE CASCADE,
    FOREIGN KEY (handled_by) REFERENCES employees (emp_id) ON DELETE SET NULL,
    CONSTRAINT chk_report_author CHECK (user_id IS NOT NULL OR employee_id IS NOT NULL)
);

-- ---------------------------------------------------------------------
--  7. FINANCE (UC_13, UC_28 - UC_35)
-- ---------------------------------------------------------------------
-- "User receives Bill". Tax (20% VAT) and total are calculated by the database.
CREATE TABLE bills (
    bill_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id        INT UNSIGNED NOT NULL,
    base_amount    DECIMAL(10, 2) NOT NULL,
    tax_amount     DECIMAL(10, 2) AS (ROUND(base_amount * 0.20, 2)) STORED,
    amount         DECIMAL(10, 2) AS (ROUND(base_amount * 1.20, 2)) STORED,
    generated_date DATE NOT NULL,
    due_date       DATE NOT NULL,
    status         ENUM('PAID', 'UNPAID', 'OVERDUE') NOT NULL DEFAULT 'UNPAID',
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_bill_amount CHECK (base_amount >= 0),
    CONSTRAINT chk_bill_dates CHECK (due_date >= generated_date)
);

CREATE TABLE expenses (
    expense_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    officer_id  INT UNSIGNED NOT NULL,
    category    VARCHAR(50) NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    recorded_at DATE NOT NULL,
    FOREIGN KEY (officer_id) REFERENCES employees (emp_id),
    CONSTRAINT chk_expense CHECK (amount > 0)
);

-- "Finance Officer reviews Financial Plan"
CREATE TABLE financial_plans (
    plan_id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    year              SMALLINT UNSIGNED NOT NULL UNIQUE,
    created_on        DATE NOT NULL,
    total_budget      DECIMAL(14, 2) NOT NULL,
    expected_expenses DECIMAL(14, 2) NOT NULL,
    expected_revenue  DECIMAL(14, 2) NOT NULL,
    reviewed_by       INT UNSIGNED NULL,
    FOREIGN KEY (reviewed_by) REFERENCES employees (emp_id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
--  8. COMMUNITY: programs, rewards, businesses, donations, apps
-- ---------------------------------------------------------------------
-- "Company Manager creates Program" (UC_15, UC_16)
CREATE TABLE programs (
    program_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    location     VARCHAR(255) NOT NULL,
    program_date DATETIME NOT NULL,
    type         ENUM('EVENT', 'EDUCATIONAL_PROGRAM') NOT NULL,
    capacity     INT UNSIGNED NOT NULL,
    created_by   INT UNSIGNED NOT NULL,
    FOREIGN KEY (created_by) REFERENCES employees (emp_id),
    CONSTRAINT chk_program_capacity CHECK (capacity > 0)
);

CREATE TABLE program_participants (
    program_id INT UNSIGNED NOT NULL,
    user_id    INT UNSIGNED NOT NULL,
    joined_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    feedback   TEXT NULL,
    PRIMARY KEY (program_id, user_id),
    FOREIGN KEY (program_id) REFERENCES programs (program_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- "Company Manager collaborates with Business", "Business offers Business Voucher"
CREATE TABLE businesses (
    business_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    location           VARCHAR(255),
    phone_number       VARCHAR(20),
    email              VARCHAR(100),
    points_per_voucher INT UNSIGNED NOT NULL DEFAULT 50
);

CREATE TABLE business_vouchers (
    voucher_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    business_id     INT UNSIGNED NOT NULL,
    user_id         INT UNSIGNED NOT NULL,
    description     VARCHAR(255) NOT NULL,
    status          ENUM('ACTIVE', 'USED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    expiration_date DATE NOT NULL,
    FOREIGN KEY (business_id) REFERENCES businesses (business_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- "Company Manager manages Donation Agency", "Donation Agency collects Donation"
CREATE TABLE donation_agencies (
    agency_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    address      VARCHAR(255),
    phone_number VARCHAR(20),
    email        VARCHAR(100)
);

-- "Donation is thrown in Smart Container"
CREATE TABLE donations (
    donation_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    citizen_id   INT UNSIGNED NOT NULL,
    container_id INT UNSIGNED NOT NULL,
    agency_id    INT UNSIGNED NULL,
    points       INT UNSIGNED NOT NULL DEFAULT 15,       -- DONATION_POINTS
    donated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    collected    BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (citizen_id) REFERENCES citizens (user_id) ON DELETE CASCADE,
    FOREIGN KEY (container_id) REFERENCES smart_containers (container_id),
    FOREIGN KEY (agency_id) REFERENCES donation_agencies (agency_id) ON DELETE SET NULL
);

-- "Company Manager chooses App" (UC_26)
CREATE TABLE apps (
    app_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    app_name  VARCHAR(100) NOT NULL UNIQUE,
    enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    chosen_by INT UNSIGNED NULL,
    FOREIGN KEY (chosen_by) REFERENCES employees (emp_id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
--  9. RECYCLING (UC_17 - UC_19)
-- ---------------------------------------------------------------------
-- "Recycling Facility Operator submits Recycling Report"
CREATE TABLE recycling_reports (
    report_id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    facility_id          INT UNSIGNED NOT NULL,
    operator_id          INT UNSIGNED NOT NULL,
    report_date          DATE NOT NULL,
    processing_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    issues_reported      TEXT,
    FOREIGN KEY (facility_id) REFERENCES recycling_facilities (facility_id),
    FOREIGN KEY (operator_id) REFERENCES recycling_facility_operators (emp_id)
);

CREATE TABLE recycling_report_items (
    report_id     INT UNSIGNED NOT NULL,
    waste_type    ENUM('PAPER', 'METAL_AND_GLASS', 'ORGANIC_WASTE', 'PLASTIC', 'HAZARDOUS') NOT NULL,
    processed_kg  DECIMAL(10, 2) NOT NULL,
    misclassified_kg DECIMAL(10, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (report_id, waste_type),
    FOREIGN KEY (report_id) REFERENCES recycling_reports (report_id) ON DELETE CASCADE
);

-- =====================================================================
--  VIEWS
-- =====================================================================

-- UC_24 Community leaderboard (Leaderboard "records for" User)
CREATE VIEW v_leaderboard AS
SELECT u.user_id,
       COALESCE(CONCAT(c.name, ' ', c.surname), u.username) AS display_name,
       u.user_type,
       u.points,
       RANK() OVER (ORDER BY u.points DESC) AS ranking
FROM users u
LEFT JOIN citizens c ON c.user_id = u.user_id;

-- UC_02 Optimal pickups per area: average daily waste x 7 / 2000 kg per truck, at least 1
CREATE VIEW v_optimal_pickups_per_area AS
SELECT area,
       ROUND(AVG(waste_kg), 1)                         AS avg_daily_kg,
       GREATEST(1, CEIL(AVG(waste_kg) * 7 / 2000))     AS pickups_per_week
FROM area_waste_statistics
GROUP BY area;

-- UC_05 What each collector has to do
CREATE VIEW v_employee_schedules AS
SELECT s.schedule_number,
       s.schedule_date,
       s.start_time,
       s.end_time,
       s.area,
       CONCAT(e.name, ' ', e.surname) AS employee,
       v.plate_number
FROM schedules s
LEFT JOIN employees e ON e.emp_id = s.employee_id
LEFT JOIN vehicles v  ON v.vehicle_id = s.vehicle_id;

-- UC_09 Bins that need emptying soon
CREATE VIEW v_bins_needing_collection AS
SELECT b.container_id,
       c.location,
       b.waste_type,
       b.status,
       ROUND(100 * c.current_fill_level / c.capacity_kg) AS fill_percent
FROM smart_bins b
JOIN smart_containers c ON c.container_id = b.container_id
WHERE b.status IN ('WARNING', 'FULL');

-- =====================================================================
--  TRIGGERS
-- =====================================================================
DELIMITER $$

-- UC_03: a collector cannot work two overlapping shifts, nor on a day off.
CREATE TRIGGER trg_schedule_check_insert
BEFORE INSERT ON schedules
FOR EACH ROW
BEGIN
    IF NEW.employee_id IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM employee_availability a
                   WHERE a.emp_id = NEW.employee_id AND a.day = NEW.schedule_date
                     AND a.availability <> 'AVAILABLE') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Employee is not available on that day';
        END IF;
        IF EXISTS (SELECT 1 FROM schedules s
                   WHERE s.employee_id = NEW.employee_id AND s.schedule_date = NEW.schedule_date
                     AND s.start_time < NEW.end_time AND NEW.start_time < s.end_time) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Employee already has a shift at that time';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_schedule_check_update
BEFORE UPDATE ON schedules
FOR EACH ROW
BEGIN
    IF NEW.employee_id IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM employee_availability a
                   WHERE a.emp_id = NEW.employee_id AND a.day = NEW.schedule_date
                     AND a.availability <> 'AVAILABLE') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Employee is not available on that day';
        END IF;
        IF EXISTS (SELECT 1 FROM schedules s
                   WHERE s.employee_id = NEW.employee_id AND s.schedule_date = NEW.schedule_date
                     AND s.schedule_number <> NEW.schedule_number
                     AND s.start_time < NEW.end_time AND NEW.start_time < s.end_time) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Employee already has a shift at that time';
        END IF;
    END IF;
END$$

-- UC_12: correct disposals add reward points and fill the bin; the bin status follows.
CREATE TRIGGER trg_disposal_after_insert
AFTER INSERT ON waste_disposals
FOR EACH ROW
BEGIN
    UPDATE users SET points = points + NEW.points_earned WHERE user_id = NEW.user_id;

    UPDATE smart_containers
       SET current_fill_level = LEAST(capacity_kg, current_fill_level + CEIL(NEW.weight_kg))
     WHERE container_id = NEW.bin_id;

    UPDATE smart_bins b
      JOIN smart_containers c ON c.container_id = b.container_id
       SET b.status = CASE
                          WHEN c.current_fill_level >= c.capacity_kg       THEN 'FULL'
                          WHEN c.current_fill_level >= 0.8 * c.capacity_kg THEN 'WARNING'
                          ELSE 'ACTIVE'
                      END
     WHERE b.container_id = NEW.bin_id;
END$$

-- UC_27: every donation is worth DONATION_POINTS reward points.
CREATE TRIGGER trg_donation_after_insert
AFTER INSERT ON donations
FOR EACH ROW
BEGIN
    UPDATE users SET points = points + NEW.points WHERE user_id = NEW.citizen_id;
END$$

DELIMITER ;
