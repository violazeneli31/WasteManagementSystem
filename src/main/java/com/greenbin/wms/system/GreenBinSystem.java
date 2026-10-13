package com.greenbin.wms.system;

import com.greenbin.wms.bins.DisposalRecord;
import com.greenbin.wms.bins.DonationContainer;
import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.bins.SmartContainer;
import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.collection.BinPlacementMap;
import com.greenbin.wms.collection.BinPlacementOrder;
import com.greenbin.wms.collection.Inventory;
import com.greenbin.wms.collection.LocationType;
import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.collection.Route;
import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.collection.WorkingZoneMap;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.community.App;
import com.greenbin.wms.community.Business;
import com.greenbin.wms.community.BusinessVoucher;
import com.greenbin.wms.community.Donation;
import com.greenbin.wms.community.DonationAgency;
import com.greenbin.wms.community.Leaderboard;
import com.greenbin.wms.community.Report;
import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.finance.Dispute;
import com.greenbin.wms.finance.Expense;
import com.greenbin.wms.finance.ExpenseTracker;
import com.greenbin.wms.finance.FinancialPlan;
import com.greenbin.wms.finance.FraudDetector;
import com.greenbin.wms.finance.PaymentMethod;
import com.greenbin.wms.finance.PricingRules;
import com.greenbin.wms.finance.TaxReport;
import com.greenbin.wms.finance.Transaction;
import com.greenbin.wms.model.BillStatus;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.ProgramType;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.notification.BaseNotification;
import com.greenbin.wms.notification.EventReminderNotification;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.notification.NotificationCategory;
import com.greenbin.wms.notification.NotificationCenter;
import com.greenbin.wms.notification.RewardNotification;
import com.greenbin.wms.notification.UrgentNotification;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.CompanyManager;
import com.greenbin.wms.people.Employee;
import com.greenbin.wms.people.FinanceOfficer;
import com.greenbin.wms.people.GarbageCollectionEmployee;
import com.greenbin.wms.people.RecyclingFacilityOperator;
import com.greenbin.wms.people.User;
import com.greenbin.wms.planning.SchedulePlanner;
import com.greenbin.wms.planning.Statistics;
import com.greenbin.wms.programs.Program;
import com.greenbin.wms.programs.ProgramFactory;
import com.greenbin.wms.recognition.ImageRecognitionService;
import com.greenbin.wms.recognition.KeywordWasteClassifier;
import com.greenbin.wms.recycling.RecyclingFacility;
import com.greenbin.wms.routing.IRouteOptimizer;
import com.greenbin.wms.routing.RouteConditions;
import com.greenbin.wms.routing.RouteOptimizationCenter;
import com.greenbin.wms.weather.SimulatedWeatherService;
import com.greenbin.wms.weather.WeatherData;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The whole system in one place: it keeps all the data (like an in-memory database)
 * and has one method for each action of the requirements, so the screens stay simple.
 * Methods are grouped by requirement (FR_xx).
 *
 * SINGLETON: the application uses one GreenBinSystem, reached with getInstance().
 */
public final class GreenBinSystem {
    public static final String CITY = "Tirana";
    public static final int FREQUENT_ERRORS = 3;           // FR_14: errors in 7 days before the manager is told
    public static final int DONATION_THRESHOLD = 10;       // FR_27: items before the manager is told

    private static GreenBinSystem instance;

    // ---------- people ----------
    private final List<User> users = new ArrayList<>();
    private final List<Employee> employees = new ArrayList<>();
    private final List<GarbageCollectionEmployee> collectors = new ArrayList<>();

    // ---------- services ----------
    private final NotificationCenter notificationCenter = new NotificationCenter();
    private final ActivityLog log = new ActivityLog();
    private final TwoFactorAuth twoFactorAuth = new TwoFactorAuth();

    // ---------- collection & planning ----------
    private WorkingZoneMap zoneMap = new WorkingZoneMap("Tirana Center", 41.30, 41.36, 19.77, 19.86);
    private final Statistics statistics = new Statistics(LocalDate.now().getYear());
    private final List<Schedule> schedules = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final SchedulePlanner planner = new SchedulePlanner(schedules, collectors, vehicles);
    private final SimulatedWeatherService weather = new SimulatedWeatherService();
    private final Set<LocalDate> weatherAlertsSent = new HashSet<>();
    private final Set<ZoneLocation> blockedLocations = new HashSet<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();

    // ---------- smart bins ----------
    private final List<SmartContainer> containers = new ArrayList<>();
    private final List<DisposalRecord> disposals = new ArrayList<>();
    private final Inventory inventory = new Inventory();
    private final BinPlacementMap binPlacementMap = new BinPlacementMap();
    private final List<String> maintenanceRequests = new ArrayList<>();

    // ---------- community ----------
    private final List<Program> programs = new ArrayList<>();
    private final List<Report> reports = new ArrayList<>();
    private final Leaderboard leaderboard = new Leaderboard(users);
    private final List<Business> businesses = new ArrayList<>();
    private final List<DonationAgency> agencies = new ArrayList<>();
    private final List<App> apps = new ArrayList<>();

    // ---------- finance ----------
    private final PricingRules pricing = new PricingRules();
    private final List<Bill> bills = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final FraudDetector fraudDetector = new FraudDetector();
    private final ExpenseTracker expenses = new ExpenseTracker();
    private final List<Dispute> disputes = new ArrayList<>();
    private final List<TaxReport> taxReports = new ArrayList<>();
    private FinancialPlan financialPlan = new FinancialPlan(LocalDate.now().getYear(), LocalDate.now(), 25_000_000);

    // ---------- recycling ----------
    private final RecyclingFacility facility = new RecyclingFacility("GreenBin Recycling Plant", "Rruga e Kosovareve, Tirana");
    private final ImageRecognitionService recognition = new ImageRecognitionService(new KeywordWasteClassifier());

    private GreenBinSystem() { }

    /** The one system used by the application, filled with demo data the first time. */
    public static synchronized GreenBinSystem getInstance() {
        if (instance == null) {
            instance = new GreenBinSystem();
            DemoData.load(instance);
        }
        return instance;
    }

    /** Starts again from the demo data ("Reset demo" button). */
    public static synchronized GreenBinSystem resetDemo() {
        instance = null;
        return getInstance();
    }

    /** An empty system that is NOT the shared instance. Used by the unit tests. */
    public static GreenBinSystem createEmpty() {
        return new GreenBinSystem();
    }

    // =====================================================================
    //  Accounts, login (NFR_13 role-based access, NFR_14 two-factor)
    // =====================================================================

    public void addUser(User user) {
        users.add(user);
        notificationCenter.subscribe(user);
    }

    public void addEmployee(Employee employee) {
        employees.add(employee);
        if (employee instanceof GarbageCollectionEmployee) collectors.add((GarbageCollectionEmployee) employee);
        if (employee instanceof RecyclingFacilityOperator) facility.getOperators().add((RecyclingFacilityOperator) employee);
        notificationCenter.subscribe(employee);
    }

    /** Returns the account, or null if the username/password are wrong. */
    public Account login(String username, String password) {
        for (Employee e : employees) if (e.login(username, password)) return e;
        for (User u : users) if (u.login(username, password)) return u;
        return null;
    }

    public boolean needsTwoFactor(Account account) {
        return account instanceof CompanyManager || account instanceof FinanceOfficer;
    }

    public CompanyManager getManager() {
        for (Employee e : employees) if (e instanceof CompanyManager) return (CompanyManager) e;
        return null;
    }

    public FinanceOfficer getFinanceOfficer() {
        for (Employee e : employees) if (e instanceof FinanceOfficer) return (FinanceOfficer) e;
        return null;
    }

    public RecyclingFacilityOperator getOperator() {
        return facility.getOperators().isEmpty() ? null : facility.getOperators().get(0);
    }

    private void notifyManager(INotification n) {
        if (getManager() != null) notificationCenter.notify(getManager(), n);
    }

    private void notifyFinance(INotification n) {
        if (getFinanceOfficer() != null) notificationCenter.notify(getFinanceOfficer(), n);
    }

    // =====================================================================
    //  FR_01 Working zone map
    // =====================================================================

    /** Adds a marker to the map. Returns null if it worked, otherwise the error message. */
    public String addLocation(String name, LocationType type, Coordinate c, Set<WasteType> categories) {
        ZoneLocation location = new ZoneLocation(name == null ? "" : name.trim(), type, c, categories);
        String error = zoneMap.validate(location);
        if (error != null) return error;
        zoneMap.addLocation(location);
        log.add("MAP", "Added " + type + " '" + location.getName() + "' - routes will use it from now on");
        return null;
    }

    public void removeLocation(ZoneLocation location) {
        zoneMap.removeLocation(location);
        blockedLocations.remove(location);
        log.add("MAP", "Removed '" + location.getName() + "' from the map");
    }

    public ZoneLocation getDepot() {
        List<ZoneLocation> depots = zoneMap.getLocations(LocationType.DEPOT);
        return depots.isEmpty() ? zoneMap.getLocations().get(0) : depots.get(0);
    }

    // =====================================================================
    //  FR_02 Statistics-based schedules  /  FR_05 notifications for collection
    // =====================================================================

    public LocalDate nextMonday() {
        LocalDate d = LocalDate.now().plusDays(1);
        while (d.getDayOfWeek() != DayOfWeek.MONDAY) d = d.plusDays(1);
        return d;
    }

    public List<Schedule> suggestWeek(LocalDate weekStart) {
        return planner.suggestCollectionSchedules(statistics, weekStart);
    }

    /** The manager confirms the suggested (and maybe edited) schedules. Returns how many were saved. */
    public int confirmSchedules(List<Schedule> suggested) {
        int saved = 0;
        for (Schedule s : suggested) {
            schedules.add(s);
            if (!planner.checkScheduleValidity(s)) {
                schedules.remove(s);
                continue;
            }
            s.setStatus(Schedule.Status.CONFIRMED);
            notifyEmployeeOfShift(s, "New collection shift");
            saved++;
        }
        log.add("SCHEDULE", saved + " schedules confirmed from statistics (" + (suggested.size() - saved) + " need manual work)");
        return saved;
    }

    /** FR_02 step 5 / FR_03: the manager enters a shift by hand. Returns the problems (empty = saved). */
    public List<String> addManualSchedule(String area, LocalDate date, LocalTime start, LocalTime end,
                                          GarbageCollectionEmployee employee, Vehicle vehicle) {
        Schedule s = new Schedule(area, date, start, end);
        s.setEmployee(employee);
        s.setAssignedVehicle(vehicle);
        List<String> problems = planner.findProblems(s);
        if (problems.isEmpty()) {
            s.setStatus(Schedule.Status.CONFIRMED);
            schedules.add(s);
            notifyEmployeeOfShift(s, "New collection shift");
            log.add("SCHEDULE", "Manual schedule added: " + s.showSchedule());
        }
        return problems;
    }

    /** FR_05: the manager gives a schedule to an employee. Returns the problems (empty = done). */
    public List<String> assignEmployee(Schedule s, GarbageCollectionEmployee employee) {
        GarbageCollectionEmployee old = s.getEmployee();
        s.setEmployee(employee);
        List<String> problems = planner.findProblems(s);
        if (!problems.isEmpty()) {
            s.setEmployee(old);
            return problems;
        }
        s.setStatus(Schedule.Status.CONFIRMED);
        s.setNote("");
        if (old != null && old != employee) {
            notificationCenter.notify(old, BaseNotification.forEmployee("Shift removed",
                    "You are no longer assigned to " + s.showSchedule(), NotificationCategory.SCHEDULE));
        }
        notifyEmployeeOfShift(s, "Collection assignment");
        log.add("SCHEDULE", "Assigned " + employee + " to schedule #" + s.getScheduleNumber());
        return problems;
    }

    private void notifyEmployeeOfShift(Schedule s, String subject) {
        if (s.getEmployee() == null) return;
        String truck = s.getAssignedVehicle() == null ? "" : " with truck " + s.getAssignedVehicle();
        notificationCenter.notify(s.getEmployee(), BaseNotification.forEmployee(subject,
                s.showSchedule() + truck, NotificationCategory.SCHEDULE));
    }

    public List<Schedule> getSchedulesOf(GarbageCollectionEmployee e) {
        List<Schedule> result = new ArrayList<>();
        for (Schedule s : schedules) if (s.getEmployee() == e) result.add(s);
        result.sort((a, b) -> a.getDate().equals(b.getDate()) ? a.getStartTime().compareTo(b.getStartTime()) : a.getDate().compareTo(b.getDate()));
        return result;
    }

    public void setEmployeeAvailability(Employee e, LocalDate date, boolean available) {
        e.setAvailability(date, available);
        log.add("STAFF", e + (available ? " is available on " : " is on leave on ") + date);
    }

    // =====================================================================
    //  FR_03 Schedule and plan  +  FR_07 weather-based adjustment
    // =====================================================================

    /** FR_07 steps 1-2: looks at the forecast and alerts the manager about extreme weather (once per day). */
    public List<WeatherData> checkWeatherAlerts() {
        List<WeatherData> severe = new ArrayList<>();
        for (WeatherData w : weather.getWeeklyForecast(CITY, LocalDate.now())) {
            if (!w.isSevere()) continue;
            severe.add(w);
            if (weatherAlertsSent.add(w.getDate())) {
                notifyManager(new UrgentNotification(BaseNotification.forEmployee("Weather alert: " + w.getCondition(),
                        w + ". Check the collections planned that day.", NotificationCategory.WEATHER)));
                log.add("WEATHER", "Alert sent: " + w);
            }
        }
        return severe;
    }

    /**
     * FR_03 / FR_07: schedules on days with extreme weather.
     * If the manager is present they are marked NEEDS_MANAGER so the manager decides;
     * if the manager is away the system moves them automatically to the next good day.
     */
    public List<String> adjustForWeather() {
        List<String> changes = new ArrayList<>();
        boolean managerPresent = getManager() != null && getManager().isPresent();
        for (Schedule s : schedules) {
            if (s.getDate().isBefore(LocalDate.now()) || s.getStatus() == Schedule.Status.DONE) continue;
            if (!weather.isSevereWeatherExpected(CITY, s.getDate())) continue;
            WeatherData w = weather.getDailyForecast(CITY, s.getDate());
            if (managerPresent) {
                s.setStatus(Schedule.Status.NEEDS_MANAGER);
                s.setNote(w.getCondition() + " expected");
                changes.add("#" + s.getScheduleNumber() + " needs your decision (" + w.getCondition() + ")");
            } else {
                LocalDate newDay = findGoodDayFor(s);
                if (newDay == null) {
                    s.setStatus(Schedule.Status.NEEDS_MANAGER);
                    changes.add("#" + s.getScheduleNumber() + " could not be moved automatically");
                    continue;
                }
                LocalDate oldDay = s.getDate();
                s.moveTo(newDay);
                s.setStatus(Schedule.Status.MOVED);
                s.setNote("Moved from " + oldDay + " (" + w.getCondition() + ")");
                notifyEmployeeOfShift(s, "Shift moved because of " + w.getCondition());
                changes.add("#" + s.getScheduleNumber() + " moved automatically " + oldDay + " -> " + newDay);
            }
        }
        for (String c : changes) log.add("WEATHER", c);
        if (!managerPresent) changes.addAll(autoAssignPendingPickups());
        return changes;
    }

    /** The next day without bad weather when the employee is free and the truck is not used. */
    public LocalDate findGoodDayFor(Schedule s) {
        LocalDate original = s.getDate();
        LocalDate found = planner.findNextDay(original, d -> {
            if (weather.isSevereWeatherExpected(CITY, d)) return false;
            s.moveTo(d);
            boolean ok = planner.checkScheduleValidity(s);
            s.moveTo(original);
            return ok;
        });
        s.moveTo(original);
        return found;
    }

    /** FR_03 step 5-7: the manager moves a shift. Returns the problems (empty = moved). */
    public List<String> moveSchedule(Schedule s, LocalDate newDate) {
        LocalDate old = s.getDate();
        s.moveTo(newDate);
        List<String> problems = planner.findProblems(s);
        if (weather.isSevereWeatherExpected(CITY, newDate)) problems.add("The weather is also extreme on " + newDate + ".");
        if (!problems.isEmpty()) {
            s.moveTo(old);
            return problems;
        }
        s.setStatus(Schedule.Status.MOVED);
        s.setNote("Moved by manager from " + old);
        notifyEmployeeOfShift(s, "Shift moved");
        log.add("SCHEDULE", "Manager moved #" + s.getScheduleNumber() + " " + old + " -> " + newDate);
        return problems;
    }

    /** FR_03 step 8: when the manager is away, pending pickup requests go to the least busy free collector. */
    public List<String> autoAssignPendingPickups() {
        List<String> changes = new ArrayList<>();
        for (Pickup p : pickups) {
            if (p.getStatus() != Pickup.Status.REQUESTED) continue;
            Schedule probe = new Schedule("probe", p.getDate(), p.getTime(), p.getTime().plusHours(1));
            GarbageCollectionEmployee e = planner.pickEmployee(probe, schedules);
            if (e != null) {
                respondToPickup(p, e, true, "Assigned automatically");
                changes.add("Pickup for " + p.getUser().getDisplayName() + " given to " + e);
            }
        }
        return changes;
    }

    // =====================================================================
    //  FR_04 Route optimization  /  FR_06 vehicle tracking
    // =====================================================================

    /** Builds a route; if strategy is null the Route Optimization Center chooses one for the conditions. */
    public Route buildRoute(LocalDate date, List<ZoneLocation> stops, RouteConditions conditions, IRouteOptimizer strategy) {
        RouteOptimizationCenter center = RouteOptimizationCenter.getInstance();
        conditions.getBlocked().addAll(blockedLocations);
        conditions.setSevereWeather(conditions.isSevereWeather() || weather.isSevereWeatherExpected(CITY, date));
        center.setStrategy(strategy != null ? strategy : center.chooseStrategy(conditions));
        return center.buildRoute(date, getDepot(), stops, conditions);
    }

    /** FR_04 step 7: the manager confirms the route; the truck starts following it and the driver is told. */
    public void confirmRoute(Route route, Vehicle vehicle, GarbageCollectionEmployee driver) {
        route.setVehicle(vehicle);
        route.setDriver(driver);
        routes.removeIf(r -> r.getVehicle() == vehicle);
        routes.add(route);
        vehicle.followPath(route.getOptimizedRoute(), false);
        if (driver != null) {
            notificationCenter.notify(driver, BaseNotification.forEmployee("New route for truck " + vehicle,
                    route.showRoute() + String.format(" (%.1f km, about %d min)", route.getTotalDistanceKm(),
                            route.getEstimatedTime().toMinutes()), NotificationCategory.SCHEDULE));
        }
        log.add("ROUTE", route.getStrategyName() + " route confirmed for " + vehicle + ": " + route.showRoute());
    }

    public void setBlocked(ZoneLocation location, boolean blocked) {
        if (blocked) blockedLocations.add(location); else blockedLocations.remove(location);
        log.add("ROUTE", "Road to '" + location.getName() + "' " + (blocked ? "BLOCKED" : "open again"));
    }

    public Route getRouteOf(Vehicle v) {
        for (Route r : routes) if (r.getVehicle() == v) return r;
        return null;
    }

    public Route getRouteOf(GarbageCollectionEmployee e) {
        for (Route r : routes) if (r.getDriver() == e) return r;
        return null;
    }

    /** One "second" of live tracking: every truck drives a bit (FR_06). */
    public void tick() {
        for (Vehicle v : vehicles) v.drive();
    }

    // =====================================================================
    //  FR_08 Barcode, FR_12 Reward points, FR_14 Error handling
    // =====================================================================

    public DisposalRecord disposeWaste(User user, SmartBin bin, WasteType realType, double kg) {
        if (user.isBlocked()) throw new IllegalStateException("This account is blocked. Please contact the finance office.");
        DisposalRecord record = bin.dispose(user, new Waste(realType, kg));
        disposals.add(record);
        if (record.getPoints() > 0) {
            transactions.add(new Transaction(user, Transaction.Type.POINTS_AWARD, record.getPoints(),
                    "Disposal at " + bin.getLocation(), LocalDateTime.now()));
        }

        if (record.isCorrect()) {
            INotification n = BaseNotification.forUser("Thank you for recycling!",
                    String.format("%.1f kg of %s in the right bin at %s.", kg, realType, bin.getLocation()), NotificationCategory.REWARDS);
            notificationCenter.notify(user, new RewardNotification(n, record.getPoints(), user.getPoints()));
        } else {
            INotification n = BaseNotification.forUser("Wrong bin",
                    realType + " does not go in the " + bin.getWasteType() + " bin. Next time use a " + realType + " bin."
                            + (realType.isHazardous() ? " Hazardous waste needs a special pickup!" : ""),
                    NotificationCategory.BIN_ALERTS);
            if (record.getPoints() > 0) n = new RewardNotification(n, record.getPoints(), user.getPoints());
            notificationCenter.notify(user, n);
            int errors = bin.countRecentErrors(7);
            log.add("BIN", "Wrong waste in " + bin + " (" + errors + " this week)");
            if (errors >= FREQUENT_ERRORS) {
                notifyManager(new UrgentNotification(BaseNotification.forEmployee("Frequent errors at " + bin.getLocation(),
                        errors + " wrong disposals in the " + bin.getWasteType() + " bin this week. "
                                + "Consider an awareness campaign or a clearer sign.", NotificationCategory.BIN_ALERTS)));
            }
        }
        checkContainer(bin);
        return record;
    }

    /** FR_08 step 3: kg thrown per waste type (from barcode scans). */
    public Map<WasteType, Double> getDisposedKgByType() {
        Map<WasteType, Double> result = new EnumMap<>(WasteType.class);
        for (WasteType t : WasteType.values()) result.put(t, 0.0);
        for (DisposalRecord r : disposals) result.merge(r.getWaste().getType(), r.getWaste().getWeight(), Double::sum);
        return result;
    }

    /** % of disposals that went into the right bin. */
    public double getCorrectDisposalRate() {
        if (disposals.isEmpty()) return 0;
        int ok = 0;
        for (DisposalRecord r : disposals) if (r.isCorrect()) ok++;
        return 100.0 * ok / disposals.size();
    }

    // =====================================================================
    //  FR_09 Bin and container capacity control
    // =====================================================================

    /** Reads one sensor: alert at 90%, automatic collection, maintenance request if the sensor is broken. */
    public String checkContainer(SmartContainer c) {
        double reading = c.readSensor();
        if (reading < 0) {
            String request = "Sensor of " + c.getKind() + " at " + c.getLocation() + " is not responding - technician needed";
            if (!maintenanceRequests.contains(request)) {
                maintenanceRequests.add(request);
                notifyManager(BaseNotification.forEmployee("Maintenance request", request, NotificationCategory.BIN_ALERTS));
                log.add("BIN", request);
            }
            return request;
        }
        if (!c.checkIfFull() || c.isAlertSent()) return null;
        c.setAlertSent(true);

        if (c instanceof DonationContainer) {
            notifyManager(BaseNotification.forEmployee("Donation container full",
                    c.getLocation() + " is " + Math.round(reading) + "% full. Ask an agency to collect.", NotificationCategory.DONATIONS));
            return "Donation container full";
        }
        LocalDate day = planner.findNextDay(LocalDate.now(), d -> !weather.isSevereWeatherExpected(CITY, d));
        Schedule s = planner.createShift("Empty " + c.getKind() + " at " + c.getLocation(), day == null ? nextWorkingDay() : day, new ArrayList<>());
        s.setSpecial(true);
        schedules.add(s);
        s.setStatus(s.getEmployee() == null ? Schedule.Status.PLANNED : Schedule.Status.CONFIRMED);
        notifyEmployeeOfShift(s, "Bin to empty");
        String msg = c.getKind() + " at " + c.getLocation() + " is " + Math.round(reading) + "% full - collection #"
                + s.getScheduleNumber() + " planned for " + s.getDate();
        notifyManager(new UrgentNotification(BaseNotification.forEmployee("Bin almost full", msg, NotificationCategory.BIN_ALERTS)));
        log.add("BIN", msg);
        return msg;
    }

    public List<String> readAllSensors() {
        List<String> events = new ArrayList<>();
        for (SmartContainer c : containers) {
            String e = checkContainer(c);
            if (e != null) events.add(e);
        }
        return events;
    }

    /** Live demo: bins fill a little over time. */
    public List<String> simulateSensorTick(Random random) {
        for (SmartContainer c : containers) {
            if (c instanceof SmartBin) c.updateFillLevel(random.nextDouble() * c.getCapacity() * 0.04);
        }
        return readAllSensors();
    }

    /** The collector empties a bin; its waste goes to the recycling plant (flagged waste separately, FR_17). */
    public List<Waste> emptyBin(SmartBin bin) {
        List<Waste> load = bin.empty();
        facility.receiveFromVehicle(load);
        double kg = 0;
        for (Waste w : load) kg += w.getWeight();
        log.add("BIN", String.format("Emptied %s (%.1f kg taken to the recycling plant)", bin, kg));
        return load;
    }

    public LocalDate nextWorkingDay() {
        LocalDate d = LocalDate.now().plusDays(1);
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        return d;
    }

    public List<SmartBin> getBins() {
        List<SmartBin> bins = new ArrayList<>();
        for (SmartContainer c : containers) if (c instanceof SmartBin) bins.add((SmartBin) c);
        return bins;
    }

    public List<DonationContainer> getDonationContainers() {
        List<DonationContainer> list = new ArrayList<>();
        for (SmartContainer c : containers) if (c instanceof DonationContainer) list.add((DonationContainer) c);
        return list;
    }

    // =====================================================================
    //  FR_11 Smart bin placement
    // =====================================================================

    /** The manager orders a new bin. Returns null if there is no bin of that type in stock. */
    public BinPlacementOrder orderBinPlacement(ZoneLocation location, WasteType type) {
        if (!inventory.takeBin(type)) {
            notifyManager(BaseNotification.forEmployee("Out of stock", "No " + type + " bins left in the warehouse.", NotificationCategory.BIN_ALERTS));
            return null;
        }
        Schedule probe = new Schedule("probe", nextWorkingDay(), LocalTime.of(6, 0), LocalTime.of(9, 0));
        GarbageCollectionEmployee e = planner.pickEmployee(probe, schedules);
        if (e == null && !collectors.isEmpty()) e = collectors.get(0);
        BinPlacementOrder order = new BinPlacementOrder(location, type, e);
        binPlacementMap.addOrder(order);
        if (e != null) {
            notificationCenter.notify(e, BaseNotification.forEmployee("Bin placement order #" + order.getOrderNumber(),
                    "Place a " + type + " bin at " + location.getName() + ".", NotificationCategory.SCHEDULE));
        }
        log.add("BIN", "Placement ordered: " + order + " -> " + e);
        return order;
    }

    /** The employee confirms the bin is in place; it appears on the map with a new barcode. */
    public SmartBin confirmBinPlacement(BinPlacementOrder order) {
        order.setStatus(BinPlacementOrder.Status.PLACED);
        Coordinate c = order.getLocation().getCoordinate();
        Coordinate spot = new Coordinate(c.getLatitude() + 0.0004 * (containers.size() % 3), c.getLongitude() + 0.0004);
        SmartBin bin = new SmartBin(order.getLocation().getName(), spot, 150, 100000000L + containers.size() + 1, order.getWasteType());
        containers.add(bin);
        notifyManager(BaseNotification.forEmployee("Bin placed", order + " confirmed by " + order.getAssignedTo(), NotificationCategory.BIN_ALERTS));
        log.add("BIN", "Placed: " + order);
        return bin;
    }

    /** Something went wrong (restricted area, missing bin...): the manager must act. */
    public void reportPlacementIssue(BinPlacementOrder order, String issue) {
        order.setStatus(BinPlacementOrder.Status.ISSUE);
        order.setIssue(issue);
        inventory.updateInventory(order.getWasteType(), 1);
        notifyManager(new UrgentNotification(BaseNotification.forEmployee("Problem with " + order,
                issue, NotificationCategory.BIN_ALERTS)));
        log.add("BIN", "Placement issue: " + order + " - " + issue);
    }

    // =====================================================================
    //  FR_13 Dynamic pricing
    // =====================================================================

    /** Changes a price; bills not yet approved are recalculated and their citizens told. Returns how many. */
    public int changePrice(WasteType type, double newPrice, FinanceOfficer officer) {
        pricing.setPrice(type, newPrice, officer.getDisplayName());
        int updated = 0;
        for (Bill b : bills) {
            Pickup p = b.getPickup();
            if (p == null || b.isApproved() || p.getTypeOfWaste() != type) continue;
            b.setBaseAmount(pricing.calculateCost(type, p.getEstimatedWeightKg()));
            notificationCenter.notify(b.getUser(), BaseNotification.forUser("Price updated",
                    String.format("The cost of your %s pickup is now %.2f Lek + tax.", type, b.getBaseAmount()), NotificationCategory.BILLING));
            updated++;
        }
        log.add("PRICING", type + " price set to " + newPrice + " Lek/kg (" + updated + " bills updated)");
        return updated;
    }

    // =====================================================================
    //  FR_15 Events, FR_16 Educational programs (FACTORY)
    // =====================================================================

    public Program createProgram(ProgramType type, String name, String description, String location,
                                 LocalDateTime date, int capacity, String extra) {
        Program p = ProgramFactory.forType(type).createProgram(name, description, location, date, capacity, extra);
        programs.add(p);
        INotification n = BaseNotification.forUser("New " + type.toString().toLowerCase() + ": " + p.getName(),
                p.getDescription() + " You are invited!", NotificationCategory.EVENTS);
        int sent = notificationCenter.notifyAll(users, new EventReminderNotification(n, p));
        log.add("PROGRAM", "Created " + type + " '" + p.getName() + "' and invited " + sent + " users");
        return p;
    }

    public boolean joinProgram(User user, Program p) {
        if (!p.addParticipant(user)) return false;
        notificationCenter.notify(user, new EventReminderNotification(BaseNotification.forUser("You are registered",
                "See you at " + p.getName() + "!", NotificationCategory.EVENTS), p));
        return true;
    }

    // =====================================================================
    //  FR_20 Reports & feedback
    // =====================================================================

    public Report submitReport(Account author, String subject, String description) {
        Report r = new Report(author, subject, description, LocalDateTime.now());
        reports.add(0, r);
        if (author instanceof User) ((User) author).getReports().add(r);
        notifyManager(BaseNotification.forEmployee("New report: " + r.getSubject(),
                "From " + author.getDisplayName() + " - sent to " + r.getCategory().getDepartment(), NotificationCategory.REPORTS));
        log.add("REPORT", "New report '" + r.getSubject() + "' -> " + r.getCategory().getDepartment());
        return r;
    }

    public void assignReport(Report r, String assignee) {
        r.setStatus(Report.Status.ASSIGNED);
        r.setAssignedTo(assignee);
        notificationCenter.notify(r.getAuthor(), BaseNotification.forUser("Your report is being handled",
                "'" + r.getSubject() + "' was assigned to " + assignee + ".", NotificationCategory.REPORTS));
    }

    public void resolveReport(Report r, String response) {
        r.setStatus(Report.Status.RESOLVED);
        r.setResponse(response);
        notificationCenter.notify(r.getAuthor(), BaseNotification.forUser("Your report was resolved",
                "'" + r.getSubject() + "': " + response, NotificationCategory.REPORTS));
        log.add("REPORT", "Resolved '" + r.getSubject() + "'");
    }

    /** FR_20 step 6: this month's reports by category. */
    public Map<Report.Category, Integer> monthlyFeedbackSummary() {
        Map<Report.Category, Integer> result = new EnumMap<>(Report.Category.class);
        for (Report.Category c : Report.Category.values()) result.put(c, 0);
        YearMonth now = YearMonth.now();
        for (Report r : reports) {
            if (YearMonth.from(r.getTimestamp()).equals(now)) result.merge(r.getCategory(), 1, Integer::sum);
        }
        return result;
    }

    // =====================================================================
    //  FR_23 Pickup requests  +  FR_28 automated billing
    // =====================================================================

    public Pickup requestPickup(User user, WasteType type, LocalDate date, LocalTime time, String address, double kg) {
        if (date.isBefore(LocalDate.now().plusDays(1))) throw new IllegalArgumentException("Pickups can be booked from tomorrow on.");
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) throw new IllegalArgumentException("There are no pickups on Sunday.");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("Please write the address.");
        ZoneLocation near = zoneMap.findByName(user.getCommunity());      // the user's neighbourhood on the map
        Coordinate c = near == null ? new Coordinate(41.33, 19.82) : near.getCoordinate();
        Pickup p = new Pickup(user, type, date, time, address.trim(), c, kg);
        pickups.add(p);
        user.getPickups().add(p);
        notificationCenter.notifyAll(collectors, BaseNotification.forEmployee("New pickup request",
                type + " at " + p.getLocation() + " on " + date + " " + time + " - confirm or deny", NotificationCategory.PICKUP));
        notifyManager(BaseNotification.forEmployee("Special pickup requested", p.showPickup(), NotificationCategory.PICKUP));
        log.add("PICKUP", user.getDisplayName() + " requested " + p.showPickup());
        return p;
    }

    /** A collector (or the system) confirms or denies a request. Confirmation adds a shift and creates the bill. */
    public void respondToPickup(Pickup p, GarbageCollectionEmployee collector, boolean accept, String reason) {
        if (p.getStatus() != Pickup.Status.REQUESTED) return;
        p.setHandledBy(collector);
        p.setAnswer(reason == null ? "" : reason);
        if (!accept) {
            p.setStatus(Pickup.Status.DENIED);
            notificationCenter.notify(p.getUser(), BaseNotification.forUser("Pickup request denied",
                    "Your " + p.getTypeOfWaste() + " pickup on " + p.getDate() + " was denied: " + reason, NotificationCategory.PICKUP));
            log.add("PICKUP", collector + " denied pickup for " + p.getUser().getDisplayName());
            return;
        }
        p.setStatus(Pickup.Status.CONFIRMED);
        Schedule s = new Schedule("Pickup: " + p.getLocation(), p.getDate(), p.getTime(), p.getTime().plusHours(1));
        s.setEmployee(collector);
        s.setSpecial(true);
        s.setStatus(Schedule.Status.CONFIRMED);
        schedules.add(s);
        notificationCenter.notify(p.getUser(), BaseNotification.forUser("Pickup confirmed",
                "We will collect your " + p.getTypeOfWaste() + " on " + p.getDate() + " at " + p.getTime()
                        + ". Collector: " + collector.getDisplayName(), NotificationCategory.PICKUP));
        generateBillForPickup(p);
        log.add("PICKUP", collector + " confirmed pickup for " + p.getUser().getDisplayName());
    }

    /** FR_28: the bill is calculated from the price rules; the finance officer must approve it. */
    public Bill generateBillForPickup(Pickup p) {
        double base = pricing.calculateCost(p.getTypeOfWaste(), p.getEstimatedWeightKg());
        Bill b = new Bill(p.getUser(), String.format("Special pickup: %.0f kg %s", p.getEstimatedWeightKg(), p.getTypeOfWaste()),
                base, LocalDate.now(), LocalDate.now().plusDays(30));
        b.setPickup(p);
        p.setBill(b);
        bills.add(b);
        p.getUser().getBills().add(b);
        notifyFinance(BaseNotification.forEmployee("Bill to approve",
                String.format("%s - %.2f Lek for %s", b.getDescription(), b.getAmount(), p.getUser().getDisplayName()), NotificationCategory.BILLING));
        return b;
    }

    public boolean approveBill(Bill b) {
        if (!b.checkBill()) {
            notifyFinance(BaseNotification.forEmployee("Missing bill data", "Bill " + b.shortId() + " is incomplete.", NotificationCategory.BILLING));
            return false;
        }
        b.approve();
        notificationCenter.notify(b.getUser(), BaseNotification.forUser("New bill",
                String.format("%s: %.2f Lek (incl. 20%% tax), due %s. Pay it in the Bills screen.", b.getDescription(), b.getAmount(), b.getDueDate()),
                NotificationCategory.BILLING));
        log.add("BILLING", "Bill " + b.shortId() + " approved and sent to " + b.getUser().getDisplayName());
        return true;
    }

    // =====================================================================
    //  FR_29 Pay bill  +  FR_31 fraud detection
    // =====================================================================

    public Transaction payBill(Bill b, PaymentMethod method) {
        Transaction t = b.payBill(method);
        if (t == null) return null;
        transactions.add(t);
        notificationCenter.notify(b.getUser(), BaseNotification.forUser("Payment receipt",
                String.format("Paid %.2f Lek by %s for bill %s. Thank you!", b.getAmount(), method, b.shortId()), NotificationCategory.BILLING));
        log.add("BILLING", b.getUser().getDisplayName() + " paid bill " + b.shortId());
        runFraudCheck();
        return t;
    }

    public void updateBillStatuses() {
        for (Bill b : bills) b.updateStatus(LocalDate.now());
    }

    /** FR_31 step 2: checks new transactions; the finance officer gets an alert for each suspicious one. */
    public List<Transaction> runFraudCheck() {
        List<Transaction> suspicious = fraudDetector.scan(transactions);
        for (Transaction t : suspicious) {
            notifyFinance(new UrgentNotification(BaseNotification.forEmployee("Possible fraud",
                    t.getAlertReason() + " - " + t.getUser().getDisplayName(), NotificationCategory.BILLING)));
            log.add("FRAUD", "Suspicious: " + t.getAlertReason() + " (" + t.getUser().getDisplayName() + ")");
        }
        return suspicious;
    }

    /** FR_31 step 3-4: fraud confirmed - the user is blocked and the transaction reversed. */
    public void confirmFraud(Transaction t) {
        t.setFraudStatus(Transaction.FraudStatus.FRAUD);
        User u = t.getUser();
        u.setBlocked(true);
        if (t.getType() == Transaction.Type.PAYMENT) {
            Transaction refund = new Transaction(u, Transaction.Type.REFUND, t.getAmount(), "Reversal of duplicate payment", LocalDateTime.now());
            refund.setFraudStatus(Transaction.FraudStatus.OK);
            transactions.add(refund);
        } else {
            u.usePoints((int) Math.min(u.getPoints(), t.getAmount()));
        }
        notificationCenter.notify(u, BaseNotification.forUser("Account blocked",
                "Your account was blocked after a fraud check. Contact the finance office.", NotificationCategory.BILLING));
        log.add("FRAUD", "Confirmed fraud for " + u.getDisplayName() + " - account blocked, transaction reversed");
    }

    public void markLegitimate(Transaction t) {
        t.setFraudStatus(Transaction.FraudStatus.LEGITIMATE);
        log.add("FRAUD", "Marked legitimate: " + t.getAlertReason());
    }

    // =====================================================================
    //  FR_25 Business vouchers, FR_26 apps & ads, FR_27 donations
    // =====================================================================

    public BusinessVoucher redeemPoints(User user, Business business, Business.Offer offer) {
        BusinessVoucher v = business.generateVoucher(user, offer);
        if (v == null) return null;
        transactions.add(new Transaction(user, Transaction.Type.VOUCHER_REDEMPTION, offer.getPointsCost(),
                offer.getDescription() + " at " + business.getName(), LocalDateTime.now()));
        notificationCenter.notify(user, BaseNotification.forUser("Your voucher is ready",
                v.getDescription() + " at " + business.getName() + ". Code: " + v.getCode(), NotificationCategory.REWARDS));
        runFraudCheck();
        return v;
    }

    /** FR_26: the ads a citizen sees, based on the waste type they recycle the most. */
    public List<App.Advertisement> getAdsFor(User user) {
        Map<WasteType, Double> kg = new EnumMap<>(WasteType.class);
        for (DisposalRecord r : user.getDisposalHistory()) kg.merge(r.getWaste().getType(), r.getWaste().getWeight(), Double::sum);
        WasteType favourite = null;
        for (Map.Entry<WasteType, Double> e : kg.entrySet()) {
            if (favourite == null || e.getValue() > kg.get(favourite)) favourite = e.getKey();
        }
        List<App.Advertisement> result = new ArrayList<>();
        for (App app : apps) {
            for (App.Advertisement ad : app.getAdsFor(favourite)) {
                ad.show();
                result.add(ad);
            }
        }
        return result;
    }

    public void setAppEnabled(App app, boolean enabled) {
        if (enabled) app.enableApp(); else app.disableApp();
        log.add("APPS", app.getAppName() + (enabled ? " enabled" : " disabled"));
    }

    public Donation donate(User user, DonationContainer container, String itemCategory) {
        Donation d = new Donation(user, itemCategory);
        container.addDonation(d);
        d.updateTotalPoints();
        transactions.add(new Transaction(user, Transaction.Type.POINTS_AWARD, Donation.DONATION_POINTS,
                "Donation: " + itemCategory, LocalDateTime.now()));
        notificationCenter.notify(user, new RewardNotification(BaseNotification.forUser("Thank you for donating",
                itemCategory + " left at " + container.getLocation() + ".", NotificationCategory.DONATIONS),
                Donation.DONATION_POINTS, user.getPoints()));
        if (countWaiting(container) == DONATION_THRESHOLD) {
            notifyManager(BaseNotification.forEmployee("Donations ready", DONATION_THRESHOLD + " items are waiting at "
                    + container.getLocation() + ". Contact a donation agency.", NotificationCategory.DONATIONS));
        }
        checkContainer(container);
        return d;
    }

    public int countWaiting(DonationContainer c) {
        int n = 0;
        for (Donation d : c.getDonations()) if (d.getStatus() == Donation.Status.DROPPED_OFF) n++;
        return n;
    }

    /** FR_27 step 4-5: the agency collects; donors are told where their items went. */
    public int sendToAgency(DonationContainer container, DonationAgency agency) {
        List<Donation> waiting = new ArrayList<>();
        for (Donation d : container.getDonations()) if (d.getStatus() == Donation.Status.DROPPED_OFF) waiting.add(d);
        if (!agency.collectDonations(waiting)) return 0;
        container.resetContainer();
        for (Donation d : waiting) {
            notificationCenter.notify(d.getCitizen(), BaseNotification.forUser("Your donation is on its way",
                    "Your " + d.getItemCategory() + " was collected by " + agency.getName() + " and will help a family in need.",
                    NotificationCategory.DONATIONS));
        }
        log.add("DONATION", waiting.size() + " items from " + container.getLocation() + " collected by " + agency);
        return waiting.size();
    }

    public int markDistributed(DonationAgency agency) {
        int n = 0;
        for (DonationContainer c : getDonationContainers()) {
            for (Donation d : c.getDonations()) {
                if (d.getAgency() == agency && d.getStatus() == Donation.Status.COLLECTED_BY_AGENCY) {
                    d.setStatus(Donation.Status.DISTRIBUTED);
                    notificationCenter.notify(d.getCitizen(), BaseNotification.forUser("Impact report",
                            "Your " + d.getItemCategory() + " was given to a family by " + agency.getName() + ". Thank you!",
                            NotificationCategory.DONATIONS));
                    n++;
                }
            }
        }
        return n;
    }

    // =====================================================================
    //  FR_32 Expenses, FR_33 Tax report, FR_34 Disputes
    // =====================================================================

    public void addExpense(Expense.Category category, String description, double amount, LocalDate date) {
        expenses.inputExpense(new Expense(category, description, amount, date));
        log.add("EXPENSE", String.format("%s: %,.0f Lek (%s)", category, amount, description));
    }

    /** FR_33: collects the data of all paid bills, recycling sales and expenses of a year. */
    public TaxReport generateTaxReport(int year) {
        double revenue = 0, vat = 0;
        for (Bill b : bills) {
            if (b.getStatus() == BillStatus.PAID && b.getGeneratedDate().getYear() == year) {
                revenue += b.getBaseAmount();
                vat += b.getTaxAmount();
            }
        }
        revenue += facility.getRevenue();
        double incentives = expenses.total(Expense.Category.RECYCLING_INCENTIVES, year);
        double operational = expenses.total(year) - incentives;
        TaxReport report = new TaxReport(year, revenue, vat, incentives, operational);
        taxReports.add(0, report);
        log.add("TAX", "Tax report " + year + " generated");
        return report;
    }

    public Dispute submitDispute(User user, Dispute.Type type, Bill bill, String description, String evidence, double amount) {
        Dispute d = new Dispute(user, type, bill, description, evidence, amount);
        d.setAssignedOfficer(getFinanceOfficer());
        d.setStatus(Dispute.Status.UNDER_REVIEW);
        disputes.add(0, d);
        notifyFinance(BaseNotification.forEmployee("New dispute", user.getDisplayName() + ": " + description, NotificationCategory.BILLING));
        notificationCenter.notify(user, BaseNotification.forUser("Dispute received",
                "We are checking your complaint. A finance officer will answer soon.", NotificationCategory.BILLING));
        log.add("DISPUTE", type + " from " + user.getDisplayName());
        return d;
    }

    /** FR_34 step 3: valid claims are corrected (bill lowered, refund or points back) and the user is told. */
    public void resolveDispute(Dispute d, boolean valid, String note) {
        d.setResolutionNote(note);
        d.setStatus(valid ? Dispute.Status.APPROVED : Dispute.Status.REJECTED);
        User u = d.getUser();
        if (valid) {
            switch (d.getType()) {
                case REWARD_POINTS:
                    u.addPoints((int) d.getClaimedAmount());
                    break;
                case BILLING_ERROR:
                    if (d.getBill() != null && d.getBill().getStatus() != BillStatus.PAID) {
                        d.getBill().setBaseAmount(Math.max(0, d.getBill().getBaseAmount() - d.getClaimedAmount()));
                        break;
                    }
                    // a paid bill gets a refund, same as a failed payment
                case FAILED_PAYMENT:
                    Transaction refund = new Transaction(u, Transaction.Type.REFUND, d.getClaimedAmount(), "Dispute refund", LocalDateTime.now());
                    refund.setFraudStatus(Transaction.FraudStatus.OK);
                    transactions.add(refund);
                    break;
                default:
                    break;
            }
        }
        notificationCenter.notify(u, BaseNotification.forUser("Dispute " + (valid ? "approved" : "rejected"),
                note, NotificationCategory.BILLING));
        log.add("DISPUTE", "Dispute of " + u.getDisplayName() + " " + d.getStatus());
    }

    /** FR_34 step 4: the manager sees which kind of dispute happens most. */
    public Map<Dispute.Type, Integer> disputeTrends() {
        Map<Dispute.Type, Integer> result = new LinkedHashMap<>();
        for (Dispute.Type t : Dispute.Type.values()) result.put(t, 0);
        for (Dispute d : disputes) result.merge(d.getType(), 1, Integer::sum);
        return result;
    }

    // =====================================================================
    //  Getters
    // =====================================================================

    public List<User> getUsers() { return users; }
    public List<Employee> getEmployees() { return employees; }
    public List<GarbageCollectionEmployee> getCollectors() { return collectors; }
    public NotificationCenter getNotificationCenter() { return notificationCenter; }
    public ActivityLog getLog() { return log; }
    public TwoFactorAuth getTwoFactorAuth() { return twoFactorAuth; }
    public WorkingZoneMap getZoneMap() { return zoneMap; }
    public void setZoneMap(WorkingZoneMap map) { this.zoneMap = map; }
    public Statistics getStatistics() { return statistics; }
    public List<Schedule> getSchedules() { return schedules; }
    public List<Vehicle> getVehicles() { return vehicles; }
    public SchedulePlanner getPlanner() { return planner; }
    public SimulatedWeatherService getWeather() { return weather; }
    public Set<ZoneLocation> getBlockedLocations() { return blockedLocations; }
    public List<Route> getRoutes() { return routes; }
    public List<Pickup> getPickups() { return pickups; }
    public List<SmartContainer> getContainers() { return containers; }
    public List<DisposalRecord> getDisposals() { return disposals; }
    public Inventory getInventory() { return inventory; }
    public BinPlacementMap getBinPlacementMap() { return binPlacementMap; }
    public List<String> getMaintenanceRequests() { return maintenanceRequests; }
    public List<Program> getPrograms() { return programs; }
    public List<Report> getReports() { return reports; }
    public Leaderboard getLeaderboard() { return leaderboard; }
    public List<Business> getBusinesses() { return businesses; }
    public List<DonationAgency> getAgencies() { return agencies; }
    public List<App> getApps() { return apps; }
    public PricingRules getPricing() { return pricing; }
    public List<Bill> getBills() { return bills; }
    public List<Transaction> getTransactions() { return transactions; }
    public ExpenseTracker getExpenses() { return expenses; }
    public List<Dispute> getDisputes() { return disputes; }
    public List<TaxReport> getTaxReports() { return taxReports; }
    public FinancialPlan getFinancialPlan() { return financialPlan; }
    public void setFinancialPlan(FinancialPlan plan) { this.financialPlan = plan; }
    public RecyclingFacility getFacility() { return facility; }
    public ImageRecognitionService getRecognition() { return recognition; }
}
