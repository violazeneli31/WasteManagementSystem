-- =====================================================================
--  Waste Management System - sample data
--  Run after schema.sql:   mysql -u root -p waste_management < sample_data.sql
--  Dates are relative to today, so the schedules are always in the future.
--  Passwords are stored as SHA-256 hashes (sample password for everyone: "password").
-- =====================================================================
USE waste_management;

-- ---------------- employees ----------------
INSERT INTO employees (name, surname, username, password_hash, email, phone_number, wage, role, supervisor_id) VALUES
('Viola', 'Zeneli', 'viola.manager', SHA2('password', 256), 'viola@greenbin.al', '+355690000001', 120000, 'MANAGER',            NULL),
('Ardi',  'Hoxha',  'ardi',          SHA2('password', 256), 'ardi@greenbin.al',  '+355690000002',  60000, 'GARBAGE_COLLECTOR',  1),
('Elira', 'Kola',   'elira',         SHA2('password', 256), 'elira@greenbin.al', '+355690000003',  60000, 'GARBAGE_COLLECTOR',  1),
('Gent',  'Basha',  'gent',          SHA2('password', 256), 'gent@greenbin.al',  '+355690000004',  60000, 'GARBAGE_COLLECTOR',  1),
('Mira',  'Dervishi','mira',         SHA2('password', 256), 'mira@greenbin.al',  '+355690000005',  80000, 'FINANCE_OFFICER',    1),
('Toni',  'Gjoka',  'toni',          SHA2('password', 256), 'toni@greenbin.al',  '+355690000006',  70000, 'RECYCLING_OPERATOR', 1);

INSERT INTO garbage_collection_employees (emp_id, shift_time) VALUES
(2, '06:00-14:00'), (3, '06:00-14:00'), (4, '14:00-22:00');

INSERT INTO recycling_facilities (name, address) VALUES
('GreenBin Recycling Plant', 'Rruga e Kosovareve, Tirana');

INSERT INTO recycling_facility_operators (emp_id, facility_id) VALUES (6, 1);

-- Elira is on leave next Monday (UC_02 / UC_05: the planner must skip her)
INSERT INTO employee_availability (emp_id, day, availability) VALUES
(3, CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, 'ON_LEAVE');

-- ---------------- users ----------------
INSERT INTO users (username, password_hash, email, phone_number, address, user_type) VALUES
('ana.m',     SHA2('password', 256), 'ana@mail.al',       '+355690000010', 'Rr. Kavajes 10, Tirana',  'CITIZEN'),
('ben.l',     SHA2('password', 256), 'ben@mail.al',       '+355690000011', 'Rr. Durresit 5, Tirana',  'CITIZEN'),
('klea.p',    SHA2('password', 256), 'klea@mail.al',      '+355690000012', 'Rr. Myslym Shyri 3',      'CITIZEN'),
('school.21', SHA2('password', 256), 'info@school21.edu.al', '+35542000021', 'Rr. Sami Frasheri 21', 'INSTITUTION');

INSERT INTO citizens (user_id, name, surname) VALUES
(1, 'Ana', 'Marku'), (2, 'Ben', 'Leka'), (3, 'Klea', 'Prifti');

INSERT INTO institutions (user_id, nipt) VALUES (4, 'K12345678A');

INSERT INTO user_notification_preferences (user_id, category, enabled) VALUES
(2, 'REWARD', FALSE);   -- Ben does not want reward notifications (FR_21)

-- ---------------- UC_01 working zone map ----------------
INSERT INTO working_zone_maps (zone_name, configured_by) VALUES ('Tirana Center', 1);

INSERT INTO zone_locations (map_id, name, latitude, longitude, location_type) VALUES
(1, 'Skanderbeg Square', 41.327500, 19.818900, 'PICKUP_POINT'),
(1, 'Blloku',            41.319700, 19.816600, 'PICKUP_POINT'),
(1, 'Pazari i Ri',       41.330200, 19.825900, 'PICKUP_POINT'),
(1, 'Tirana e Re',       41.337000, 19.812000, 'PICKUP_POINT'),
(1, 'Kombinat',          41.319600, 19.783400, 'PICKUP_POINT'),
(1, 'School 21',         41.324500, 19.829800, 'INSTITUTION'),
(1, 'Recycling Plant',   41.345000, 19.800000, 'RECYCLING_FACILITY');

-- ---------------- UC_02 statistics (last 5 days) ----------------
INSERT INTO area_waste_statistics (area, record_date, waste_kg) VALUES
('Blloku',      CURDATE() - INTERVAL 5 DAY, 620), ('Blloku',      CURDATE() - INTERVAL 4 DAY, 580),
('Blloku',      CURDATE() - INTERVAL 3 DAY, 700), ('Blloku',      CURDATE() - INTERVAL 2 DAY, 650),
('Blloku',      CURDATE() - INTERVAL 1 DAY, 610),
('Kombinat',    CURDATE() - INTERVAL 5 DAY, 240), ('Kombinat',    CURDATE() - INTERVAL 4 DAY, 260),
('Kombinat',    CURDATE() - INTERVAL 3 DAY, 230), ('Kombinat',    CURDATE() - INTERVAL 2 DAY, 250),
('Kombinat',    CURDATE() - INTERVAL 1 DAY, 220),
('Pazari i Ri', CURDATE() - INTERVAL 5 DAY, 980), ('Pazari i Ri', CURDATE() - INTERVAL 4 DAY, 1050),
('Pazari i Ri', CURDATE() - INTERVAL 3 DAY, 1010), ('Pazari i Ri', CURDATE() - INTERVAL 2 DAY, 990),
('Pazari i Ri', CURDATE() - INTERVAL 1 DAY, 1020);

-- ---------------- vehicles ----------------
INSERT INTO vehicles (plate_number, fuel_level, current_latitude, current_longitude) VALUES
('AA 123 BB', 85, 41.327500, 19.818700),
('AB 456 CD', 60, 41.327500, 19.818700);

INSERT INTO vehicle_assignments (vehicle_id, emp_id) VALUES (1, 2), (2, 3), (2, 4);

-- ---------------- UC_02/UC_03 schedules for next week ----------------
-- (next Monday = CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY)
INSERT INTO schedules (employee_id, vehicle_id, schedule_date, start_time, end_time, area, created_by) VALUES
(2,    1, CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY,  '06:00', '09:00', 'Blloku',      1),
(2,    1, CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY,  '09:00', '12:00', 'Pazari i Ri', 1),
(3,    2, CURDATE() + INTERVAL (9 - WEEKDAY(CURDATE())) DAY,  '09:00', '12:00', 'Blloku',      1),
(4,    2, CURDATE() + INTERVAL (9 - WEEKDAY(CURDATE())) DAY,  '14:00', '17:00', 'Pazari i Ri', 1),
(NULL, NULL, CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '06:00', '09:00', 'Kombinat',  1);  -- not assigned yet

-- ---------------- UC_04 an optimized route ----------------
INSERT INTO routes (driver_id, vehicle_id, schedule_number, route_date, strategy, total_distance_km) VALUES
(2, 1, 1, CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, 'NORMAL_ROUTINE', 6.77);

INSERT INTO route_stops (route_id, stop_order, latitude, longitude) VALUES
(1, 0, 41.327500, 19.818700),   -- depot
(1, 1, 41.327500, 19.818900),
(1, 2, 41.319700, 19.816600),
(1, 3, 41.330200, 19.825900),
(1, 4, 41.337000, 19.812000),
(1, 5, 41.319600, 19.783400);

-- ---------------- UC_23 special pickup ----------------
INSERT INTO pickups (user_id, waste_type, pickup_date, location, is_special) VALUES
(1, 'HAZARDOUS', CURDATE() + INTERVAL (9 - WEEKDAY(CURDATE())) DAY, 'Rr. Kavajes 10, Tirana', TRUE);

-- ---------------- UC_05 notifications ----------------
INSERT INTO notifications (type, category, subject, description, sender_id, recipient_emp_id) VALUES
('EMPLOYEE_COLLECTION', 'SCHEDULE_UPDATE', 'Collection assignment', 'You are assigned to schedule #1 (Blloku, 06:00-09:00)', 1, 2),
('EMPLOYEE_COLLECTION', 'SCHEDULE_UPDATE', 'Collection assignment', 'You are assigned to schedule #2 (Pazari i Ri, 09:00-12:00)', 1, 2);

INSERT INTO notifications (type, category, subject, description, sender_id) VALUES
('USER_INFORMATIVE', 'EVENT_REMINDER', 'Earth Day Clean-up', 'Help us clean the Grand Park of Tirana!', 1);

-- ---------------- smart bins and disposals ----------------
INSERT INTO smart_containers (location, capacity_kg, barcode_serial, container_kind) VALUES
('Skanderbeg Square', 200, 100000001, 'SMART_BIN'),
('Skanderbeg Square', 200, 100000002, 'SMART_BIN'),
('Blloku',            150, 100000003, 'SMART_BIN'),
('Rr. Durresit',      100, 100000004, 'DONATION_CONTAINER');

INSERT INTO smart_bins (container_id, waste_type) VALUES
(1, 'PLASTIC'), (2, 'PAPER'), (3, 'METAL_AND_GLASS');

-- The trigger adds the points to the user and fills the bin.
INSERT INTO waste_disposals (bin_id, user_id, waste_type, weight_kg, points_earned) VALUES
(1, 1, 'PLASTIC',         4.0, 12),
(2, 2, 'PAPER',           3.0,  6),
(3, 3, 'METAL_AND_GLASS', 5.0, 15),
(1, 4, 'PLASTIC',       160.0, 480);   -- the school's recycling drive: the bin goes to WARNING

INSERT INTO waste_reports (bin_id, subject, description) VALUES
(1, 'Incorrect waste', '2.0 kg PAPER thrown into the PLASTIC bin at Skanderbeg Square');

INSERT INTO inventory (waste_type, bins_in_stock) VALUES
('PAPER', 12), ('METAL_AND_GLASS', 8), ('ORGANIC_WASTE', 10), ('PLASTIC', 0), ('HAZARDOUS', 3);

INSERT INTO bin_placements (latitude, longitude, waste_type) VALUES
(41.324500, 19.829800, 'PAPER'),
(41.324500, 19.829800, 'PLASTIC');

-- ---------------- reports, finance ----------------
INSERT INTO reports (user_id, subject, description) VALUES
(2, 'Feedback from Ben Leka', 'The paper bin on Rr. Durresit is often full on Fridays.');
INSERT INTO reports (employee_id, subject, description, status, handled_by) VALUES
(2, 'Collector Ardi Hoxha', 'Road works on Rr. Myslym Shyri block the usual route.', 'IN_PROGRESS', 1);

INSERT INTO bills (user_id, base_amount, generated_date, due_date, status) VALUES
(1, 1000.00, CURDATE() - INTERVAL 40 DAY, CURDATE() - INTERVAL 10 DAY, 'OVERDUE'),
(2,  450.00, CURDATE() - INTERVAL 5 DAY,  CURDATE() + INTERVAL 25 DAY, 'UNPAID'),
(4, 3200.00, CURDATE() - INTERVAL 20 DAY, CURDATE() + INTERVAL 10 DAY, 'PAID');

INSERT INTO expenses (officer_id, category, amount, recorded_at) VALUES
(5, 'Fuel', 85000, CURDATE() - INTERVAL 7 DAY),
(5, 'Truck maintenance', 42000, CURDATE() - INTERVAL 3 DAY);

INSERT INTO financial_plans (year, created_on, total_budget, expected_expenses, expected_revenue, reviewed_by) VALUES
(YEAR(CURDATE()), CURDATE() - INTERVAL 60 DAY, 25000000, 21000000, 26500000, 5);

-- ---------------- programs, businesses, donations, apps ----------------
INSERT INTO programs (name, description, location, program_date, type, capacity, created_by) VALUES
('Earth Day Clean-up', 'Help us clean the Grand Park of Tirana.', 'Grand Park of Tirana',
 TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '10:00:00'), 'EVENT', 100, 1),
('Recycling 101', 'How to sort waste at home and at school.', 'School 21',
 TIMESTAMP(CURDATE() + INTERVAL 20 DAY, '11:00:00'), 'EDUCATIONAL_PROGRAM', 30, 1);

INSERT INTO program_participants (program_id, user_id) VALUES (1, 1), (1, 3), (2, 4);

INSERT INTO businesses (name, location, phone_number, email, points_per_voucher) VALUES
('Green Cafe', 'Blloku, Tirana', '+355692222222', 'hello@greencafe.al', 50);

INSERT INTO business_vouchers (business_id, user_id, description, expiration_date) VALUES
(1, 4, 'Free coffee at Green Cafe', CURDATE() + INTERVAL 30 DAY);

INSERT INTO donation_agencies (name, address, phone_number, email) VALUES
('Albanian Red Cross', 'Rr. Muhamet Gjollesha, Tirana', '+35542257321', 'info@kksh.org.al');

INSERT INTO donations (citizen_id, container_id, agency_id) VALUES (3, 4, 1);   -- +15 points for Klea

INSERT INTO apps (app_name, enabled, chosen_by) VALUES ('e-Albania', TRUE, 1), ('Tirana City App', FALSE, NULL);

-- ---------------- recycling ----------------
INSERT INTO recycling_reports (facility_id, operator_id, report_date, processing_confirmed, issues_reported) VALUES
(1, 6, CURDATE() - INTERVAL 1 DAY, TRUE, 'Conveyor belt 2 stopped for 30 minutes');

INSERT INTO recycling_report_items (report_id, waste_type, processed_kg, misclassified_kg) VALUES
(1, 'PLASTIC', 1250, 40), (1, 'PAPER', 980, 15), (1, 'METAL_AND_GLASS', 640, 22);
