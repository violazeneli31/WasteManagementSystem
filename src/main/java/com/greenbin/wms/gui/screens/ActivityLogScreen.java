package com.greenbin.wms.gui.screens;

import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.system.ActivityLog;

import javax.swing.JComboBox;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.util.TreeSet;

/** Everything the system decided or logged: schedules, weather decisions, prices, alerts, fraud... */
public class ActivityLogScreen extends Screen {
    private final DefaultTableModel model = Tables.model("Time", "Area", "What happened");
    private final JComboBox<String> filter = new JComboBox<>();

    public ActivityLogScreen(Account me) {
        super(me, "Activity Log", "The system logs its decisions for tracking and future analysis (FR_02, FR_07, FR_13...)", "Log");
        filter.addActionListener(e -> fill());
        body().add(row(muted("Area:"), filter), BorderLayout.NORTH);
        javax.swing.JTable t = Theme.newTable(model);
        t.getColumnModel().getColumn(0).setMaxWidth(140);
        t.getColumnModel().getColumn(1).setMaxWidth(120);
        body().add(Theme.table(t), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Object sel = filter.getSelectedItem();
        TreeSet<String> cats = new TreeSet<>();
        for (ActivityLog.Entry e : sys.getLog().getEntries()) cats.add(e.category);
        filter.removeAllItems();
        filter.addItem("ALL");
        for (String c : cats) filter.addItem(c);
        if (sel != null) filter.setSelectedItem(sel);
        fill();
    }

    private void fill() {
        model.setRowCount(0);
        Object f = filter.getSelectedItem();
        for (ActivityLog.Entry e : sys.getLog().getEntries()) {
            if (f == null || "ALL".equals(f) || e.category.equals(f)) {
                model.addRow(new Object[]{e.time.format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")), e.category, e.message});
            }
        }
    }
}
