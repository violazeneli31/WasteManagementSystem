package com.greenbin.wms.system;

import com.greenbin.wms.bins.DonationContainer;
import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.collection.LocationType;
import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.collection.Route;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.community.App;
import com.greenbin.wms.community.Business;
import com.greenbin.wms.community.DonationAgency;
import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.finance.Dispute;
import com.greenbin.wms.finance.Expense;
import com.greenbin.wms.finance.FinancialPlan;
import com.greenbin.wms.finance.PaymentMethod;
import com.greenbin.wms.finance.Transaction;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.ProgramType;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Citizen;
import com.greenbin.wms.people.CompanyManager;
import com.greenbin.wms.people.FinanceOfficer;
import com.greenbin.wms.people.GarbageCollectionEmployee;
import com.greenbin.wms.people.Institution;
import com.greenbin.wms.people.RecyclingFacilityOperator;
import com.greenbin.wms.people.User;
import com.greenbin.wms.programs.EducationalProgram;
import com.greenbin.wms.programs.Program;
import com.greenbin.wms.recycling.RecyclingFacility;
import com.greenbin.wms.routing.RouteConditions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Example data so every screen has something to show when the app starts.
 * The same people and places are used in database/sample_data.sql.
 * Every account has the password "password".
 */
public final class DemoData {
    public static final String PASSWORD = "password";

    private DemoData() { }

    public static void load(GreenBinSystem s) {
        // ---------------- employees ----------------
        CompanyManager viola = new CompanyManager("Viola", "Zeneli", "viola", PASSWORD, "+355690000001", "viola@greenbin.al", 120000);
        GarbageCollectionEmployee ardi = new GarbageCollectionEmployee("Ardi", "Hoxha", "ardi", PASSWORD, "+355690000002", "ardi@greenbin.al", 60000, "06:00-14:00");
        GarbageCollectionEmployee elira = new GarbageCollectionEmployee("Elira", "Kola", "elira", PASSWORD, "+355690000003", "elira@greenbin.al", 60000, "06:00-14:00");
        GarbageCollectionEmployee gent = new GarbageCollectionEmployee("Gent", "Basha", "gent", PASSWORD, "+355690000004", "gent@greenbin.al", 60000, "14:00-22:00");
        FinanceOfficer mira = new FinanceOfficer("Mira", "Dervishi", "mira", PASSWORD, "+355690000005", "mira@greenbin.al", 80000);
        RecyclingFacilityOperator toni = new RecyclingFacilityOperator("Toni", "Gjoka", "toni", PASSWORD, "+355690000006", "toni@greenbin.al", 70000, "GreenBin Recycling Plant");
        s.addEmployee(viola);
        s.addEmployee(ardi);
        s.addEmployee(elira);
        s.addEmployee(gent);
        s.addEmployee(mira);
        s.addEmployee(toni);
        elira.setAvailability(s.nextMonday(), false);           // Elira is on leave next Monday

        // ---------------- users ----------------
        Citizen ana = new Citizen("Ana", "Marku", "ana", PASSWORD, "ana@mail.al", "+355690000010", "Rr. Kavajes 10, Tirana", "Blloku");
        Citizen ben = new Citizen("Ben", "Leka", "ben", PASSWORD, "ben@mail.al", "+355690000011", "Rr. Durresit 5, Tirana", "Kombinat");
        Citizen klea = new Citizen("Klea", "Prifti", "klea", PASSWORD, "klea@mail.al", "+355690000012", "Rr. Myslym Shyri 3, Tirana", "Pazari i Ri");
        Institution school = new Institution("School 21", "K12345678A", "school21", PASSWORD, "info@school21.edu.al", "+35542000021", "Rr. Sami Frasheri 21", "Pazari i Ri");
        Citizen dritan = new Citizen("Dritan", "Shehu", "dritan", PASSWORD, "dritan@mail.al", "+355690000013", "Rr. Ismail Qemali 7", "Blloku");
        Citizen sara = new Citizen("Sara", "Gjika", "sara", PASSWORD, "sara@mail.al", "+355690000014", "Rr. Siri Kodra 40", "Tirana e Re");
        Citizen luan = new Citizen("Luan", "Kraja", "luan", PASSWORD, "luan@mail.al", "+355690000015", "Rr. Teodor Keko 12", "Kombinat");
        User[] all = {ana, ben, klea, school, dritan, sara, luan};
        int[] startPoints = {180, 95, 240, 620, 150, 60, 130};
        for (int i = 0; i < all.length; i++) {
            s.addUser(all[i]);
            all[i].addPoints(startPoints[i]);
        }
        ben.setNotificationPreference(com.greenbin.wms.notification.NotificationCategory.REWARDS, false);   // FR_21

        // ---------------- FR_01 working zone map ----------------
        EnumSet<WasteType> allTypes = EnumSet.allOf(WasteType.class);
        EnumSet<WasteType> recyclables = EnumSet.of(WasteType.PAPER, WasteType.PLASTIC, WasteType.METAL_AND_GLASS);
        addLoc(s, "GreenBin Depot", LocationType.DEPOT, 41.3425, 19.8060, allTypes);
        addLoc(s, "Skanderbeg Square", LocationType.PICKUP_POINT, 41.3275, 19.8189, allTypes);
        addLoc(s, "Blloku", LocationType.PICKUP_POINT, 41.3197, 19.8166, allTypes);
        addLoc(s, "Pazari i Ri", LocationType.PICKUP_POINT, 41.3302, 19.8259, allTypes);
        addLoc(s, "Tirana e Re", LocationType.PICKUP_POINT, 41.3370, 19.8120, allTypes);
        addLoc(s, "Kombinat", LocationType.PICKUP_POINT, 41.3196, 19.7834, allTypes);
        addLoc(s, "Ali Demi", LocationType.PICKUP_POINT, 41.3240, 19.8370, allTypes);
        addLoc(s, "21 Dhjetori", LocationType.PICKUP_POINT, 41.3268, 19.8025, recyclables);
        addLoc(s, "Komuna e Parisit", LocationType.PICKUP_POINT, 41.3168, 19.7985, allTypes);
        addLoc(s, "Lapraka", LocationType.PICKUP_POINT, 41.3385, 19.7905, allTypes);
        addLoc(s, "Selita", LocationType.PICKUP_POINT, 41.3108, 19.8125, allTypes);
        addLoc(s, "Rruga e Elbasanit", LocationType.PICKUP_POINT, 41.3175, 19.8310, allTypes);
        addLoc(s, "School 21", LocationType.INSTITUTION, 41.3245, 19.8298, recyclables);
        addLoc(s, "Mother Teresa Hospital", LocationType.INSTITUTION, 41.3335, 19.8235, EnumSet.of(WasteType.HAZARDOUS, WasteType.PAPER));
        addLoc(s, "Recycling Plant", LocationType.RECYCLING_FACILITY, 41.3480, 19.7990, allTypes);
        s.getLog().getEntries().clear();

        // ---------------- FR_02 statistics: kg per day, last 14 days ----------------
        double[][] data = {
                {600, 620, 580, 640, 650, 610, 700, 630, 660, 640, 690, 650, 700, 710},
                {240, 260, 230, 250, 220, 245, 255, 235, 250, 240, 230, 260, 245, 250},
                {980, 1050, 1010, 990, 1020, 1040, 1000, 1060, 1030, 1080, 1050, 1100, 1070, 1090},
                {450, 430, 470, 460, 440, 480, 455, 470, 490, 460, 500, 480, 495, 510},
                {420, 410, 400, 390, 395, 380, 370, 385, 360, 365, 350, 355, 340, 345}};
        String[] areas = {"Blloku", "Kombinat", "Pazari i Ri", "Tirana e Re", "Ali Demi"};
        for (int a = 0; a < areas.length; a++) for (double kg : data[a]) s.getStatistics().addData(areas[a], kg);

        // ---------------- vehicles ----------------
        Coordinate depot = s.getDepot().getCoordinate();
        Vehicle v1 = new Vehicle("AA 123 BB", 85, depot);
        Vehicle v2 = new Vehicle("AB 456 CD", 60, depot);
        Vehicle v3 = new Vehicle("AC 789 EF", 95, depot);
        v1.getAssignedEmployees().add(ardi);
        v2.getAssignedEmployees().add(elira);
        v2.getAssignedEmployees().add(gent);
        s.getVehicles().add(v1);
        s.getVehicles().add(v2);
        s.getVehicles().add(v3);

        // ---------------- schedules for next week (FR_02 suggestion, confirmed) ----------------
        s.confirmSchedules(s.suggestWeek(s.nextMonday()));

        // ---------------- routes that are being driven now (FR_04, FR_06) ----------------
        Route r1 = s.buildRoute(LocalDate.now(), stops(s, "Tirana e Re", "Skanderbeg Square", "Blloku", "Komuna e Parisit", "Kombinat", "Lapraka"),
                new RouteConditions(), null);
        s.confirmRoute(r1, v1, ardi);
        v1.followPath(r1.getOptimizedRoute(), true);
        Route r2 = s.buildRoute(LocalDate.now(), stops(s, "Pazari i Ri", "Ali Demi", "Rruga e Elbasanit", "Selita", "21 Dhjetori"),
                new RouteConditions(), null);
        s.confirmRoute(r2, v2, gent);
        v2.followPath(r2.getOptimizedRoute(), true);

        // ---------------- smart bins (FR_08, FR_09) ----------------
        long serial = 100000001L;
        SmartBin skPlastic = bin(s, "Skanderbeg Square", 41.3279, 19.8185, 200, serial++, WasteType.PLASTIC, 60);
        bin(s, "Skanderbeg Square", 41.3272, 19.8195, 200, serial++, WasteType.PAPER, 35);
        SmartBin blMag = bin(s, "Blloku", 41.3200, 19.8160, 150, serial++, WasteType.METAL_AND_GLASS, 40);
        bin(s, "Blloku", 41.3193, 19.8172, 150, serial++, WasteType.ORGANIC_WASTE, 50);
        SmartBin pzPlastic = bin(s, "Pazari i Ri", 41.3306, 19.8255, 150, serial++, WasteType.PLASTIC, 70);
        SmartBin pzPaper = bin(s, "Pazari i Ri", 41.3298, 19.8264, 150, serial++, WasteType.PAPER, 20);
        bin(s, "Ali Demi", 41.3244, 19.8366, 100, serial++, WasteType.HAZARDOUS, 10);
        bin(s, "Tirana e Re", 41.3374, 19.8116, 150, serial++, WasteType.ORGANIC_WASTE, 91);
        bin(s, "Kombinat", 41.3200, 19.7830, 150, serial++, WasteType.PLASTIC, 30);
        SmartBin lapraka = bin(s, "Lapraka", 41.3389, 19.7900, 150, serial++, WasteType.METAL_AND_GLASS, 55);
        lapraka.getSensor().setWorking(false);                  // FR_09 step 5: a broken sensor
        DonationContainer durresit = new DonationContainer("Rr. Durresit", new Coordinate(41.3290, 19.8100), 40, serial++);
        DonationContainer blloku = new DonationContainer("Blloku Center", new Coordinate(41.3210, 19.8180), 40, serial);
        s.getContainers().add(durresit);
        s.getContainers().add(blloku);

        // disposals (FR_08, FR_12, FR_14): three wrong ones in the same bin trigger a manager alert
        s.disposeWaste(ana, skPlastic, WasteType.PLASTIC, 4);
        s.disposeWaste(ana, blMag, WasteType.METAL_AND_GLASS, 3);
        s.disposeWaste(klea, pzPaper, WasteType.PAPER, 5);
        s.disposeWaste(school, skPlastic, WasteType.PLASTIC, 12);
        s.disposeWaste(dritan, blMag, WasteType.METAL_AND_GLASS, 6);
        s.disposeWaste(sara, pzPaper, WasteType.PAPER, 2);
        s.disposeWaste(ben, pzPlastic, WasteType.PAPER, 2);
        s.disposeWaste(luan, pzPlastic, WasteType.ORGANIC_WASTE, 1.5);
        s.disposeWaste(sara, pzPlastic, WasteType.PAPER, 1);
        s.disposeWaste(luan, pzPlastic, WasteType.PLASTIC, 3);

        // ---------------- FR_11 inventory and placement ----------------
        s.getInventory().updateInventory(WasteType.PAPER, 12);
        s.getInventory().updateInventory(WasteType.METAL_AND_GLASS, 8);
        s.getInventory().updateInventory(WasteType.ORGANIC_WASTE, 10);
        s.getInventory().updateInventory(WasteType.HAZARDOUS, 3);
        s.orderBinPlacement(s.getZoneMap().findByName("School 21"), WasteType.PAPER);

        // ---------------- FR_15 / FR_16 programs ----------------
        Program earthDay = s.createProgram(ProgramType.EVENT, "Earth Day Clean-up", "Help us clean the Grand Park of Tirana.",
                "Grand Park of Tirana", LocalDateTime.of(LocalDate.now().plusDays(12), LocalTime.of(10, 0)), 100, "Collect 500 kg of litter");
        Program recycling101 = s.createProgram(ProgramType.EDUCATIONAL_PROGRAM, "Recycling 101", "How to sort waste at home and at school.",
                "School 21", LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.of(11, 0)), 30, "Pupils aged 10-14");
        s.joinProgram(ana, earthDay);
        s.joinProgram(klea, earthDay);
        s.joinProgram(dritan, earthDay);
        s.joinProgram(school, recycling101);

        // a program that already happened, with attendance and feedback (past data, so not made by the factory)
        Program workshop = new EducationalProgram("Plastic-Free Week Workshop", "Simple ways to use less plastic.",
                "Tirana Youth Center", LocalDateTime.now().minusDays(10), 40, "Young people");
        User[] came = {ana, ben, klea, sara, luan, dritan};
        for (User u : came) workshop.getParticipants().add(u);
        for (int i = 0; i < 4; i++) workshop.markAttendance(came[i]);
        workshop.addFeedback(ana, 5, "Very useful, I switched to reusable bags.");
        workshop.addFeedback(ben, 4, "Good, but it was a bit long.");
        workshop.addFeedback(klea, 5, "Loved the practical examples.");
        s.getPrograms().add(workshop);

        // ---------------- FR_20 reports ----------------
        s.submitReport(ben, "Paper bin often full on Fridays", "The paper bin on Rr. Durresit is full every Friday afternoon.");
        s.submitReport(ardi, "Road works", "Road works on Rr. Myslym Shyri block the usual route.");
        s.submitReport(klea, "Suggestion: more bins near the park", "It would be nice to have recycling bins at the park entrance.");
        s.assignReport(s.getReports().get(1), "Collection team");

        // ---------------- FR_23 pickups and FR_28 bills ----------------
        s.requestPickup(ana, WasteType.HAZARDOUS, workingDay(s, 3), LocalTime.of(10, 0), "Rr. Kavajes 10, Tirana", 8);
        Pickup kleaPickup = s.requestPickup(klea, WasteType.ORGANIC_WASTE, workingDay(s, 2), LocalTime.of(16, 0), "Rr. Myslym Shyri 3", 25);
        s.respondToPickup(kleaPickup, ardi, true, "");

        Bill anaBill = new Bill(ana, "Waste collection - " + YearMonth.now().minusMonths(1), 1000, LocalDate.now().minusDays(40), LocalDate.now().minusDays(10));
        Bill benBill = new Bill(ben, "Waste collection - " + YearMonth.now(), 450, LocalDate.now().minusDays(5), LocalDate.now().plusDays(25));
        for (Bill b : new Bill[]{anaBill, benBill}) {
            b.approve();
            s.getBills().add(b);
            b.getUser().getBills().add(b);
        }
        // paid monthly bills of the past months (revenue for the tax report)
        for (int m = 1; m <= 6; m++) {
            for (int u = 0; u < all.length; u++) {
                double amount = (all[u] instanceof Institution ? 3200 : 600) + 25 * m + 7 * u;
                Bill b = new Bill(all[u], "Waste collection - " + YearMonth.now().minusMonths(m), amount,
                        LocalDate.now().minusMonths(m), LocalDate.now().minusMonths(m).plusDays(30));
                b.approve();
                Transaction t = b.payBill(PaymentMethod.values()[(u + m) % 4]);
                t.setFraudStatus(Transaction.FraudStatus.OK);
                s.getTransactions().add(t);
                s.getBills().add(b);
                all[u].getBills().add(b);
            }
        }
        s.updateBillStatuses();

        // ---------------- FR_31 suspicious transactions ----------------
        s.getTransactions().add(new Transaction(luan, Transaction.Type.PAYMENT, 540, "Bill 7F3A21 by Credit card", LocalDateTime.now().minusMinutes(6)));
        s.getTransactions().add(new Transaction(luan, Transaction.Type.PAYMENT, 540, "Bill 7F3A21 by Credit card", LocalDateTime.now().minusMinutes(3)));
        s.getTransactions().add(new Transaction(dritan, Transaction.Type.POINTS_AWARD, 220, "Disposal at Blloku", LocalDateTime.now().minusHours(2)));
        s.getTransactions().add(new Transaction(dritan, Transaction.Type.POINTS_AWARD, 190, "Disposal at Blloku", LocalDateTime.now().minusHours(1)));

        // ---------------- FR_25 businesses, FR_26 apps, FR_27 donations ----------------
        Business cafe = new Business("Green Cafe", "Blloku, Tirana", "+355692222222", "hello@greencafe.al");
        cafe.addOffer("Free coffee", 50);
        cafe.addOffer("Coffee + pastry", 90);
        Business market = new Business("FreshMarket Tirana", "Rr. e Kavajes, Tirana", "+355693333333", "info@freshmarket.al");
        market.addOffer("10% discount on shopping", 120);
        market.addOffer("500 Lek shopping voucher", 300);
        Business books = new Business("EcoBooks", "Pazari i Ri, Tirana", "+355694444444", "shop@ecobooks.al");
        books.addOffer("Notebook made of recycled paper", 70);
        s.getBusinesses().add(cafe);
        s.getBusinesses().add(market);
        s.getBusinesses().add(books);
        s.redeemPoints(school, cafe, cafe.getOffers().get(0));

        App city = new App("City Services App", true);
        city.addAd(new App.Advertisement("Pay your city bills online", "All municipal bills in one place.", null));
        App ecoShop = new App("EcoShop", true);
        ecoShop.addAd(new App.Advertisement("Reusable bottles -20%", "Stop buying plastic bottles.", WasteType.PLASTIC));
        ecoShop.addAd(new App.Advertisement("Recycled notebooks", "School notebooks made from your paper.", WasteType.PAPER));
        ecoShop.addAd(new App.Advertisement("Home compost kit", "Turn your food waste into soil.", WasteType.ORGANIC_WASTE));
        ecoShop.addAd(new App.Advertisement("Glass food jars", "Keep food fresh without plastic.", WasteType.METAL_AND_GLASS));
        App events = new App("Tirana Events", false);
        events.addAd(new App.Advertisement("Concert in the park", "Free concert this Saturday.", null));
        s.getApps().add(city);
        s.getApps().add(ecoShop);
        s.getApps().add(events);

        s.getAgencies().add(new DonationAgency("Albanian Red Cross", "Rr. Muhamet Gjollesha, Tirana", "+35542257321", "info@kksh.org.al"));
        s.getAgencies().add(new DonationAgency("Tirana Community Shelter", "Rr. Dibres 100, Tirana", "+35542200200", "help@shelter.al"));
        String[] items = {"Winter jacket", "Children's books", "Toys", "Shoes", "Blanket", "School bag", "Kitchen pans", "Sweater"};
        User[] donors = {klea, ana, klea, sara, ben, ana, dritan, klea};
        for (int i = 0; i < items.length; i++) s.donate(donors[i], durresit, items[i]);

        // ---------------- FR_32 expenses, FR_35 financial plan ----------------
        double[] base = {420000, 1100000, 150000, 80000, 90000};
        Expense.Category[] cats = Expense.Category.values();
        for (int m = 5; m >= 0; m--) {
            YearMonth ym = YearMonth.now().minusMonths(m);
            LocalDate day = ym.atDay(Math.min(5, m == 0 ? LocalDate.now().getDayOfMonth() : 5));
            for (int c = 0; c < cats.length; c++) {
                double amount = base[c] * (1 + 0.02 * (5 - m));
                if (m == 0 && cats[c] == Expense.Category.VEHICLE_MAINTENANCE) amount *= 1.25;   // repairs this month
                s.getExpenses().inputExpense(new Expense(cats[c], cats[c] + " " + ym, Math.round(amount), day));
            }
        }
        FinancialPlan plan = new FinancialPlan(LocalDate.now().getYear(), LocalDate.now().minusDays(60), 25_000_000);
        for (int m = 0; m < 12; m++) plan.addMonth(2_050_000 * Math.pow(1.015, m), 1_900_000 * Math.pow(1.025, m));
        s.setFinancialPlan(plan);

        // ---------------- FR_34 dispute ----------------
        s.submitDispute(ben, Dispute.Type.REWARD_POINTS, null, "I recycled 3 kg of paper on Monday but got no points.",
                "Screenshot of the scan at 10:42", 20);

        // ---------------- FR_17-19 recycling plant ----------------
        RecyclingFacility f = s.getFacility();
        f.addToBatch("Paper", 500);
        f.addToBatch("Organic", 400);
        f.sortReceivedWaste();
        RecyclingFacility.Batch paper = f.getBatches().get(0);
        f.moveToNextStage(paper);
        f.moveToNextStage(paper);
        f.moveToNextStage(f.getBatches().get(1));
        f.addToBatch("Plastic", 1250);
        f.addToBatch(RecyclingFacility.METAL_AND_GLASS, 640);
        f.sortReceivedWaste();
        f.addToBatch("Paper", 980);
        List<Waste> flaggedLoad = new ArrayList<>();
        flaggedLoad.add(flagged(WasteType.PAPER, WasteType.PLASTIC, 2.0));
        flaggedLoad.add(flagged(WasteType.ORGANIC_WASTE, WasteType.PLASTIC, 1.5));
        flaggedLoad.add(flagged(WasteType.PAPER, WasteType.PAPER, 3.0));     // false alarm: sensor was wrong
        f.receiveFromVehicle(flaggedLoad);
        f.reportIssue("Conveyor belt 2 stopped for 30 minutes");

        // ---------------- alerts at start-up ----------------
        s.readAllSensors();
        s.checkWeatherAlerts();
        s.runFraudCheck();
    }

    private static void addLoc(GreenBinSystem s, String name, LocationType type, double lat, double lon, EnumSet<WasteType> types) {
        s.addLocation(name, type, new Coordinate(lat, lon), types);
    }

    private static List<ZoneLocation> stops(GreenBinSystem s, String... names) {
        List<ZoneLocation> list = new ArrayList<>();
        for (String n : names) list.add(s.getZoneMap().findByName(n));
        return list;
    }

    private static SmartBin bin(GreenBinSystem s, String location, double lat, double lon, double capacity,
                                long serial, WasteType type, double fillPercent) {
        SmartBin b = new SmartBin(location, new Coordinate(lat, lon), capacity, serial, type);
        b.updateFillLevel(capacity * fillPercent / 100);
        s.getContainers().add(b);
        return b;
    }

    private static Waste flagged(WasteType real, WasteType bin, double kg) {
        Waste w = new Waste(real, kg);
        w.setBinType(bin);
        w.setFlagged(true);
        return w;
    }

    /** The n-th day from today that is not a Sunday. */
    private static LocalDate workingDay(GreenBinSystem s, int n) {
        LocalDate d = LocalDate.now();
        int count = 0;
        while (count < n) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY) count++;
        }
        return d;
    }
}
