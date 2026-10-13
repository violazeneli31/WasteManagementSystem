-- =====================================================================
--  Waste Management System - example queries, one or more per use case
--  Run after schema.sql and sample_data.sql.
-- =====================================================================
USE waste_management;

-- ---------- UC_01 Working Zone Map ----------
-- All locations on the "Tirana Center" map, by type
SELECT z.zone_name, l.location_type, l.name, l.latitude, l.longitude
FROM zone_locations l
JOIN working_zone_maps z ON z.map_id = l.map_id
WHERE z.zone_name = 'Tirana Center'
ORDER BY l.location_type, l.name;

-- Is there already a location within ~10 m of a new point? (duplicate check before adding)
SELECT name, latitude, longitude
FROM zone_locations
WHERE map_id = 1
  AND ABS(latitude  - 41.32752) < 0.00009
  AND ABS(longitude - 19.81892) < 0.00012;

-- ---------- UC_02 Statistics-Based Schedule Optimization ----------
SELECT * FROM v_optimal_pickups_per_area ORDER BY pickups_per_week DESC;

-- Collectors who are free on a given day (not on leave)
SELECT e.emp_id, CONCAT(e.name, ' ', e.surname) AS collector
FROM garbage_collection_employees g
JOIN employees e ON e.emp_id = g.emp_id
WHERE NOT EXISTS (SELECT 1 FROM employee_availability a
                  WHERE a.emp_id = e.emp_id
                    AND a.day = CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY
                    AND a.availability <> 'AVAILABLE');

-- ---------- UC_03 Schedule and Plan ----------
-- Next week's plan
SELECT * FROM v_employee_schedules ORDER BY schedule_date, start_time;

-- Special pickup requests still waiting to be scheduled
SELECT p.request_id, u.username, p.waste_type, p.pickup_date, p.location
FROM pickups p
JOIN users u ON u.user_id = p.user_id
WHERE p.is_special AND p.status = 'REQUESTED';

-- Moving a shift by one day (the trigger rejects it if it clashes or the employee is off)
-- UPDATE schedules SET schedule_date = schedule_date + INTERVAL 1 DAY WHERE schedule_number = 1;

-- ---------- UC_04 Route Optimization ----------
SELECT r.route_id, r.route_date, r.strategy, r.total_distance_km,
       CONCAT(e.name, ' ', e.surname) AS driver, v.plate_number,
       s.stop_order, s.latitude, s.longitude
FROM routes r
JOIN employees e   ON e.emp_id = r.driver_id
LEFT JOIN vehicles v ON v.vehicle_id = r.vehicle_id
JOIN route_stops s ON s.route_id = r.route_id
ORDER BY r.route_id, s.stop_order;

-- ---------- UC_05 Notification for Collection ----------
-- Schedules nobody has been assigned to yet
SELECT schedule_number, schedule_date, start_time, end_time, area
FROM schedules
WHERE employee_id IS NULL;

-- Each collector's unread notifications
SELECT CONCAT(e.name, ' ', e.surname) AS collector, n.subject, n.description, n.created_at
FROM notifications n
JOIN employees e ON e.emp_id = n.recipient_emp_id
WHERE n.type = 'EMPLOYEE_COLLECTION' AND NOT n.is_read
ORDER BY collector, n.created_at;

-- ---------- Other parts of the system ----------
-- UC_24 Leaderboard (points are added by the triggers)
SELECT ranking, display_name, user_type, points FROM v_leaderboard;

-- UC_09 Bins that need emptying soon
SELECT * FROM v_bins_needing_collection;

-- Smart bin types that are out of stock
SELECT waste_type FROM inventory WHERE bins_in_stock = 0;

-- UC_28/29 Unpaid and overdue bills with the tax calculated by the database
SELECT b.bill_id, u.username, b.base_amount, b.tax_amount, b.amount, b.due_date, b.status
FROM bills b
JOIN users u ON u.user_id = b.user_id
WHERE b.status <> 'PAID'
ORDER BY b.due_date;

-- UC_15/16 Programs and how full they are
SELECT p.name, p.type, p.program_date, COUNT(pp.user_id) AS participants, p.capacity
FROM programs p
LEFT JOIN program_participants pp ON pp.program_id = p.program_id
GROUP BY p.program_id, p.name, p.type, p.program_date, p.capacity;

-- UC_18 Recycling: processed and misclassified waste per type
SELECT i.waste_type, SUM(i.processed_kg) AS processed_kg, SUM(i.misclassified_kg) AS misclassified_kg,
       ROUND(100 * SUM(i.misclassified_kg) / SUM(i.processed_kg), 1) AS misclassified_percent
FROM recycling_report_items i
GROUP BY i.waste_type;
