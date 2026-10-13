package com.greenbin.wms.gui.screens;

import com.greenbin.wms.collection.Route;
import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Inputs;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.MapView;
import com.greenbin.wms.gui.components.NotificationList;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.GarbageCollectionEmployee;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** FR_05: the collector checks their shifts, notifications and today's route before starting work. */
public class CollectorTasksScreen extends Screen {
    private final GarbageCollectionEmployee collector;
    private final DefaultTableModel model = Tables.model("#", "Date", "Time", "Area", "Truck", "Status", "Note");
    private final JTable table = Theme.newTable(model);
    private final List<Schedule> rows = new ArrayList<>();
    private final MapView map;
    private final JLabel routeInfo = Theme.label("", Theme.BOLD, Theme.TEXT);
    private final NotificationList latest = new NotificationList();

    public CollectorTasksScreen(Account me) {
        super(me, "My Tasks", "Your shifts, today's route and your latest messages", "FR_05", "FR_06");
        collector = (GarbageCollectionEmployee) me;
        table.getColumnModel().getColumn(5).setCellRenderer(new Theme.StatusRenderer());

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(560, 100));
        JPanel shifts = new JPanel(new BorderLayout(0, 6));
        shifts.setOpaque(false);
        shifts.add(Theme.table(table), BorderLayout.CENTER);
        shifts.add(row(FlatButton.primary("Log my attendance today", e -> {
            collector.logAttendance(LocalDate.now());
            info("Attendance logged for " + LocalDate.now() + ". Have a safe shift!");
        }), FlatButton.secondary("Mark selected shift done", e -> {
            int r = table.getSelectedRow();
            if (r < 0) { error("Select a shift first."); return; }
            rows.get(r).setStatus(Schedule.Status.DONE);
            sys.getLog().add("SCHEDULE", collector + " finished shift #" + rows.get(r).getScheduleNumber());
            refresh();
        })), BorderLayout.SOUTH);
        left.add(Theme.card("My shifts (shift time " + collector.getShiftTime() + ")", shifts), BorderLayout.CENTER);
        JScrollPane sp = new JScrollPane(latest);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(100, 260));
        left.add(Theme.card("Latest notifications", sp), BorderLayout.SOUTH);
        body().add(left, BorderLayout.WEST);

        map = new MapView(sys);
        JPanel mapCard = new JPanel(new BorderLayout(0, 6));
        mapCard.setOpaque(false);
        mapCard.add(routeInfo, BorderLayout.NORTH);
        mapCard.add(map, BorderLayout.CENTER);
        body().add(Theme.card("My route today (numbers = order of the stops)", mapCard), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        rows.clear();
        rows.addAll(sys.getSchedulesOf(collector));
        model.setRowCount(0);
        for (Schedule s : rows) {
            model.addRow(new Object[]{s.getScheduleNumber(), Inputs.day(s.getDate()), s.getStartTime() + "-" + s.getEndTime(), s.getArea(),
                    s.getAssignedVehicle() == null ? "-" : s.getAssignedVehicle(), s.getStatus(), s.getNote()});
        }
        Route r = sys.getRouteOf(collector);
        map.setRoute(r);
        if (r != null) map.setSelected(r.getVehicle());
        routeInfo.setText(r == null ? "No route assigned yet." : String.format("Truck %s - %s - %.1f km, about %d min",
                r.getVehicle(), r.getStrategyName(), r.getTotalDistanceKm(), r.getEstimatedTime().toMinutes()));
        List<com.greenbin.wms.notification.INotification> n = me.getNotifications();
        latest.setNotifications(n.subList(0, Math.min(6, n.size())));
    }

    @Override
    public void onTick() { map.repaint(); }
}
