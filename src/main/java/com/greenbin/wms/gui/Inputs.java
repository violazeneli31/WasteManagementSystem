package com.greenbin.wms.gui;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Ready-made input fields for dates and times. */
public final class Inputs {
    private Inputs() { }

    /** The next days (from tomorrow), shown as "Wed 21 Oct". Sundays are skipped if noSundays is true. */
    public static JComboBox<LocalDate> dateCombo(int days, boolean noSundays) {
        JComboBox<LocalDate> combo = new JComboBox<>();
        LocalDate d = LocalDate.now();
        int added = 0;
        while (added < days) {
            d = d.plusDays(1);
            if (noSundays && d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            combo.addItem(d);
            added++;
        }
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                String text = value instanceof LocalDate ? ((LocalDate) value).format(DateTimeFormatter.ofPattern("EEE dd MMM")) : "";
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        });
        return combo;
    }

    /** Times from 06:00 to 18:00 every hour. */
    public static JComboBox<LocalTime> timeCombo() {
        JComboBox<LocalTime> combo = new JComboBox<>();
        for (int h = 6; h <= 18; h++) combo.addItem(LocalTime.of(h, 0));
        combo.setPrototypeDisplayValue(LocalTime.of(18, 30));
        combo.setPreferredSize(new java.awt.Dimension(90, combo.getPreferredSize().height));
        return combo;
    }

    public static String day(LocalDate d) { return d.format(DateTimeFormatter.ofPattern("EEE dd MMM")); }
}
