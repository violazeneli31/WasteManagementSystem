package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.SmartContainer;
import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.community.Report;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.MapView;
import com.greenbin.wms.gui.components.NotificationList;
import com.greenbin.wms.gui.components.StatCard;
import com.greenbin.wms.gui.components.WeatherCard;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.CompanyManager;
import com.greenbin.wms.system.GreenBinSystem;
import com.greenbin.wms.weather.WeatherData;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The manager's start page: key numbers, live map, weather and the latest alerts. */
public class ManagerHomeScreen extends Screen {
    private final StatCard fullBins = new StatCard("bins 90% full or more", Theme.RED);
    private final StatCard shifts = new StatCard("shifts in the next 7 days", Theme.GREEN);
    private final StatCard pickups = new StatCard("pickup requests waiting", Theme.ORANGE);
    private final StatCard reports = new StatCard("open reports", Theme.BLUE);
    private final StatCard trucks = new StatCard("trucks driving now", new java.awt.Color(0xF57C00));
    private final StatCard correct = new StatCard("disposals in the right bin", Theme.PURPLE);
    private final MapView map;
    private final JPanel weather = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final NotificationList alerts = new NotificationList();
    private final JCheckBox present = new JCheckBox("I am at work (if not, the system reschedules automatically - FR_03)");

    public ManagerHomeScreen(Account me) {
        super(me, "Good day, " + ((CompanyManager) me).getName(), "Everything that needs your attention today", "Overview");
        JPanel numbers = new JPanel(new GridLayout(1, 6, 10, 0));
        numbers.setOpaque(false);
        for (StatCard c : new StatCard[]{fullBins, shifts, pickups, reports, trucks, correct}) numbers.add(c);

        map = new MapView(sys);
        JPanel left = Theme.card("Live map - trucks move in real time (FR_06)", map);

        weather.setOpaque(false);
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);
        right.add(Theme.card("Weather this week (FR_07)", weather), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(alerts);
        sp.setBorder(null);
        right.add(Theme.card("Latest notifications", sp), BorderLayout.CENTER);
        present.setOpaque(false);
        present.setFont(Theme.BOLD);
        present.addActionListener(e -> {
            ((CompanyManager) me).setPresent(present.isSelected());
            if (!present.isSelected()) {
                List<String> changes = sys.adjustForWeather();
                info("You are marked as away.\nThe system handled these automatically:\n- "
                        + (changes.isEmpty() ? "nothing to change" : String.join("\n- ", changes)));
            }
        });
        right.add(present, BorderLayout.SOUTH);
        right.setPreferredSize(new java.awt.Dimension(610, 400));

        body().add(numbers, BorderLayout.NORTH);
        body().add(left, BorderLayout.CENTER);
        body().add(right, BorderLayout.EAST);
    }

    @Override
    public void refresh() {
        GreenBinSystem s = sys;
        int full = 0;
        for (SmartContainer c : s.getContainers()) if (c.checkIfFull()) full++;
        fullBins.setValue(String.valueOf(full));
        int week = 0;
        for (Schedule sc : s.getSchedules()) {
            if (!sc.getDate().isBefore(LocalDate.now()) && sc.getDate().isBefore(LocalDate.now().plusDays(8))) week++;
        }
        shifts.setValue(String.valueOf(week));
        int waiting = 0;
        for (Pickup p : s.getPickups()) if (p.getStatus() == Pickup.Status.REQUESTED) waiting++;
        pickups.setValue(String.valueOf(waiting));
        int open = 0;
        for (Report r : s.getReports()) if (r.getStatus() != Report.Status.RESOLVED) open++;
        reports.setValue(String.valueOf(open));
        correct.setValue(Math.round(s.getCorrectDisposalRate()) + "%");
        weather.removeAll();
        for (WeatherData w : s.getWeather().getWeeklyForecast(GreenBinSystem.CITY, LocalDate.now())) weather.add(new WeatherCard(w));
        List<INotification> latest = new ArrayList<>(me.getNotifications());
        alerts.setNotifications(latest.subList(0, Math.min(8, latest.size())));
        present.setSelected(((CompanyManager) me).isPresent());
        onTick();
        revalidate();
        repaint();
    }

    @Override
    public void onTick() {
        int driving = 0;
        for (Vehicle v : sys.getVehicles()) if (v.isDriving()) driving++;
        trucks.setValue(driving + " / " + sys.getVehicles().size());
        map.repaint();
    }
}
