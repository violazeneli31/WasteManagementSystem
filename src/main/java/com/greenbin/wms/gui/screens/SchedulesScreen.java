package com.greenbin.wms.gui.screens;

import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Inputs;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.ScheduleCalendar;
import com.greenbin.wms.gui.components.WeatherCard;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.CompanyManager;
import com.greenbin.wms.people.GarbageCollectionEmployee;
import com.greenbin.wms.planning.SchedulePlanner;
import com.greenbin.wms.system.GreenBinSystem;
import com.greenbin.wms.weather.WeatherData;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR_02 confirm suggested schedules, FR_03 manage routine + special pickups,
 * FR_05 assign employees (they get notified), FR_07 weather-based adjustment.
 */
public class SchedulesScreen extends Screen {
    private final JPanel weatherRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JComboBox<LocalDate> weatherDay = Inputs.dateCombo(6, false);
    private final JComboBox<WeatherData.Condition> weatherCondition = new JComboBox<>(WeatherData.Condition.values());
    private final JCheckBox present = new JCheckBox("I am at work");

    private final ScheduleCalendar calendar = new ScheduleCalendar();
    private LocalDate weekStart;
    private final JLabel weekLabel = Theme.label("", Theme.BOLD, Theme.TEXT);

    private final DefaultTableModel shiftsModel = Tables.model("#", "Date", "Time", "Area", "Employee", "Truck", "Status", "Note");
    private final JTable shiftsTable = Theme.newTable(shiftsModel);
    private final List<Schedule> shown = new ArrayList<>();
    private final JComboBox<GarbageCollectionEmployee> employee = new JComboBox<>();
    private final JComboBox<LocalDate> moveDay = Inputs.dateCombo(14, true);

    private final DefaultTableModel suggestionModel = Tables.model("Date", "Time", "Area", "Employee", "Truck", "Note");
    private List<Schedule> suggestion = new ArrayList<>();

    private final DefaultTableModel pickupModel = Tables.model("Requested by", "Waste", "Date", "Time", "Address", "kg", "Status", "Collector");
    private final JTable pickupTable = Theme.newTable(pickupModel);
    private final JComboBox<GarbageCollectionEmployee> pickupCollector = new JComboBox<>();

    private final DefaultTableModel staffModel = Tables.model("Employee", "Shift time", "Days on leave", "Shifts planned");
    private final JTable staffTable = Theme.newTable(staffModel);
    private final JComboBox<LocalDate> leaveDay = Inputs.dateCombo(21, true);

    public SchedulesScreen(Account me) {
        super(me, "Schedules & Weather", "Plan the week, react to storms and special pickups, and assign the collectors",
                "FR_02", "FR_03", "FR_05", "FR_07");

        // ---- weather (FR_07)
        weatherRow.setOpaque(false);
        JPanel weatherCard = new JPanel(new BorderLayout(0, 6));
        weatherCard.setOpaque(false);
        weatherCard.add(weatherRow, BorderLayout.CENTER);
        present.setOpaque(false);
        present.setFont(Theme.BOLD);
        present.addActionListener(e -> ((CompanyManager) me).setPresent(present.isSelected()));
        weatherCard.add(row(muted("Simulate the forecast:"), weatherDay, weatherCondition,
                FlatButton.secondary("Set", e -> {
                    sys.getWeather().setCondition((LocalDate) weatherDay.getSelectedItem(), (WeatherData.Condition) weatherCondition.getSelectedItem());
                    sys.checkWeatherAlerts();
                    refresh();
                }),
                FlatButton.primary("Check weather & adjust schedules", e -> adjust()), present,
                muted("(not at work = system moves shifts by itself)")), BorderLayout.SOUTH);
        body().add(Theme.card("Weather forecast for " + GreenBinSystem.CITY + " - red = extreme weather", weatherCard), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BOLD);

        // ---- calendar
        JPanel cal = new JPanel(new BorderLayout(0, 6));
        cal.setOpaque(false);
        cal.add(row(FlatButton.secondary("< Previous week", e -> { weekStart = weekStart.minusWeeks(1); showWeek(); }),
                weekLabel, FlatButton.secondary("Next week >", e -> { weekStart = weekStart.plusWeeks(1); showWeek(); })), BorderLayout.NORTH);
        cal.add(calendar, BorderLayout.CENTER);
        tabs.addTab("Week calendar", cal);

        // ---- all shifts (FR_03, FR_05)
        JPanel shifts = new JPanel(new BorderLayout(0, 6));
        shifts.setOpaque(false);
        shiftsTable.getColumnModel().getColumn(6).setCellRenderer(new Theme.StatusRenderer());
        shifts.add(Theme.table(shiftsTable), BorderLayout.CENTER);
        shifts.add(row(muted("Selected shift:"), employee, FlatButton.primary("Assign employee", e -> assign()),
                moveDay, FlatButton.secondary("Move to day", e -> move()),
                FlatButton.secondary("Check for problems", e -> validateSelected()),
                FlatButton.secondary("Add a shift by hand...", e -> addManual())), BorderLayout.SOUTH);
        tabs.addTab("All shifts", shifts);

        // ---- suggestion (FR_02)
        JPanel sug = new JPanel(new BorderLayout(0, 6));
        sug.setOpaque(false);
        sug.add(row(FlatButton.primary("Suggest next week from statistics", e -> suggest()),
                FlatButton.primary("Confirm & notify employees", e -> confirmSuggestion()),
                muted("Pickups per area come from Statistics. Employees on leave are skipped; work is shared fairly.")), BorderLayout.NORTH);
        sug.add(Theme.table(Theme.newTable(suggestionModel)), BorderLayout.CENTER);
        tabs.addTab("Suggest week (FR_02)", sug);

        // ---- special pickups (FR_03 + FR_23)
        JPanel sp = new JPanel(new BorderLayout(0, 6));
        sp.setOpaque(false);
        pickupTable.getColumnModel().getColumn(6).setCellRenderer(new Theme.StatusRenderer());
        sp.add(Theme.table(pickupTable), BorderLayout.CENTER);
        sp.add(row(muted("Plan the selected request with:"), pickupCollector,
                FlatButton.primary("Plan pickup", e -> planPickup()),
                FlatButton.secondary("Let the system choose (fair workload)", e -> {
                    List<String> c = sys.autoAssignPendingPickups();
                    info(c.isEmpty() ? "No requests waiting, or nobody is free." : String.join("\n", c));
                    refresh();
                })), BorderLayout.SOUTH);
        tabs.addTab("Special pickup requests", sp);

        // ---- availability (FR_05 step 1)
        JPanel staff = new JPanel(new BorderLayout(0, 6));
        staff.setOpaque(false);
        staff.add(Theme.table(staffTable), BorderLayout.CENTER);
        staff.add(row(muted("Selected employee:"), leaveDay,
                FlatButton.danger("Set on leave", e -> setLeave(false)),
                FlatButton.primary("Set available", e -> setLeave(true))), BorderLayout.SOUTH);
        tabs.addTab("Staff availability", staff);

        body().add(tabs, BorderLayout.CENTER);
        weekStart = sys.nextMonday();
    }

    private Schedule selectedShift() {
        int r = shiftsTable.getSelectedRow();
        if (r < 0) {
            error("Select a shift in the table first.");
            return null;
        }
        return shown.get(r);
    }

    private void assign() {
        Schedule s = selectedShift();
        if (s == null) return;
        List<String> problems = sys.assignEmployee(s, (GarbageCollectionEmployee) employee.getSelectedItem());
        if (!problems.isEmpty()) error(String.join("\n", problems));
        else info(s.getEmployee() + " was assigned and notified.");
        refresh();
    }

    private void move() {
        Schedule s = selectedShift();
        if (s == null) return;
        List<String> problems = sys.moveSchedule(s, (LocalDate) moveDay.getSelectedItem());
        if (!problems.isEmpty()) error("Can't move it:\n" + String.join("\n", problems));
        else info("Shift #" + s.getScheduleNumber() + " moved. The employee was notified.");
        refresh();
    }

    private void validateSelected() {
        Schedule s = selectedShift();
        if (s == null) return;
        List<String> problems = sys.getPlanner().findProblems(s);
        if (problems.isEmpty()) info("Shift #" + s.getScheduleNumber() + " is valid: no overlaps, employee available.");
        else error(String.join("\n", problems));
    }

    private void addManual() {
        JTextField area = new JTextField("Blloku");
        JComboBox<LocalDate> day = Inputs.dateCombo(14, true);
        JComboBox<LocalTime> start = Inputs.timeCombo();
        JComboBox<GarbageCollectionEmployee> emp = new JComboBox<>(sys.getCollectors().toArray(new GarbageCollectionEmployee[0]));
        JComboBox<Vehicle> truck = new JComboBox<>(sys.getVehicles().toArray(new Vehicle[0]));
        Object[] form = {"Area", area, "Day", day, "Start (3-hour shift)", start, "Employee", emp, "Truck", truck};
        if (javax.swing.JOptionPane.showConfirmDialog(this, form, "New shift", javax.swing.JOptionPane.OK_CANCEL_OPTION) != 0) return;
        LocalTime st = (LocalTime) start.getSelectedItem();
        List<String> problems = sys.addManualSchedule(area.getText().trim(), (LocalDate) day.getSelectedItem(), st,
                st.plusHours(SchedulePlanner.SHIFT_HOURS), (GarbageCollectionEmployee) emp.getSelectedItem(), (Vehicle) truck.getSelectedItem());
        if (!problems.isEmpty()) error("The shift was not saved:\n" + String.join("\n", problems));
        refresh();
    }

    private void adjust() {
        sys.checkWeatherAlerts();
        List<String> changes = sys.adjustForWeather();
        info(changes.isEmpty() ? "No shifts fall on days with extreme weather." : String.join("\n", changes)
                + (((CompanyManager) me).isPresent() ? "\n\nShifts marked 'decide!' are waiting for you: move them in 'All shifts'." : ""));
        refresh();
    }

    private void suggest() {
        suggestion = sys.suggestWeek(sys.nextMonday());
        suggestionModel.setRowCount(0);
        for (Schedule s : suggestion) {
            suggestionModel.addRow(new Object[]{Inputs.day(s.getDate()), s.getStartTime() + "-" + s.getEndTime(), s.getArea(),
                    s.getEmployee() == null ? "-" : s.getEmployee(), s.getAssignedVehicle() == null ? "-" : s.getAssignedVehicle(), s.getNote()});
        }
    }

    private void confirmSuggestion() {
        if (suggestion.isEmpty()) {
            error("First press 'Suggest next week from statistics'.");
            return;
        }
        int saved = sys.confirmSchedules(suggestion);
        info(saved + " shifts saved and employees notified.\n" + (suggestion.size() - saved)
                + " could not be saved (overlap or nobody free) - add them by hand.");
        suggestion = new ArrayList<>();
        suggestionModel.setRowCount(0);
        refresh();
    }

    private void planPickup() {
        int r = pickupTable.getSelectedRow();
        if (r < 0) {
            error("Select a pickup request first.");
            return;
        }
        Pickup p = sys.getPickups().get(r);
        if (p.getStatus() != Pickup.Status.REQUESTED) {
            error("This request was already answered.");
            return;
        }
        GarbageCollectionEmployee e = (GarbageCollectionEmployee) pickupCollector.getSelectedItem();
        if (!e.isAvailable(p.getDate())) {
            error(e + " is on leave that day.");
            return;
        }
        sys.respondToPickup(p, e, true, "Planned by the manager");
        info("Pickup planned with " + e + ". The citizen got a confirmation and a bill was prepared.");
        refresh();
    }

    private void setLeave(boolean available) {
        int r = staffTable.getSelectedRow();
        if (r < 0) {
            error("Select an employee first.");
            return;
        }
        sys.setEmployeeAvailability(sys.getCollectors().get(r), (LocalDate) leaveDay.getSelectedItem(), available);
        refresh();
    }

    private void showWeek() {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) weekStart = weekStart.minusDays(weekStart.getDayOfWeek().getValue() - 1);
        weekLabel.setText("   Week of " + Inputs.day(weekStart) + "   ");
        calendar.setData(weekStart, sys.getSchedules(), sys.getCollectors(),
                d -> sys.getWeather().isSevereWeatherExpected(GreenBinSystem.CITY, d));
    }

    @Override
    public void refresh() {
        present.setSelected(((CompanyManager) me).isPresent());
        weatherRow.removeAll();
        for (WeatherData w : sys.getWeather().getWeeklyForecast(GreenBinSystem.CITY, LocalDate.now())) weatherRow.add(new WeatherCard(w));
        weatherRow.revalidate();
        weatherRow.repaint();

        employee.removeAllItems();
        pickupCollector.removeAllItems();
        for (GarbageCollectionEmployee e : sys.getCollectors()) {
            employee.addItem(e);
            pickupCollector.addItem(e);
        }

        shown.clear();
        shown.addAll(sys.getSchedules());
        shown.sort((a, b) -> a.getDate().equals(b.getDate()) ? a.getStartTime().compareTo(b.getStartTime()) : a.getDate().compareTo(b.getDate()));
        shiftsModel.setRowCount(0);
        for (Schedule s : shown) {
            shiftsModel.addRow(new Object[]{s.getScheduleNumber(), Inputs.day(s.getDate()), s.getStartTime() + "-" + s.getEndTime(),
                    s.getArea(), s.getEmployee() == null ? "NOT ASSIGNED" : s.getEmployee(),
                    s.getAssignedVehicle() == null ? "-" : s.getAssignedVehicle(), s.getStatus(), s.getNote()});
        }

        pickupModel.setRowCount(0);
        for (Pickup p : sys.getPickups()) {
            pickupModel.addRow(new Object[]{p.getUser().getDisplayName(), p.getTypeOfWaste(), Inputs.day(p.getDate()), p.getTime(),
                    p.getLocation(), p.getEstimatedWeightKg(), p.getStatus(), p.getHandledBy() == null ? "-" : p.getHandledBy()});
        }

        staffModel.setRowCount(0);
        for (GarbageCollectionEmployee e : sys.getCollectors()) {
            List<String> days = new ArrayList<>();
            for (LocalDate d : e.getDaysOff()) days.add(Inputs.day(d));
            staffModel.addRow(new Object[]{e, e.getShiftTime(), days.isEmpty() ? "-" : String.join(", ", days),
                    sys.getPlanner().countShifts(e, sys.getSchedules())});
        }
        showWeek();
    }
}
