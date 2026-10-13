package com.greenbin.wms.gui.components;

import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.people.GarbageCollectionEmployee;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** A week view (Monday-Saturday, 06:00-18:00) with every shift drawn as a coloured block. */
public class ScheduleCalendar extends JComponent {
    private static final Color[] EMPLOYEE_COLORS = {
            new Color(0x43A047), new Color(0x1E88E5), new Color(0x8E24AA), new Color(0x00897B), new Color(0x6D4C41)};

    private List<Schedule> schedules = new ArrayList<>();
    private List<GarbageCollectionEmployee> employees = new ArrayList<>();
    private LocalDate weekStart = LocalDate.now();
    private Predicate<LocalDate> badWeather = d -> false;

    public ScheduleCalendar() { setPreferredSize(new Dimension(700, 380)); }

    public void setData(LocalDate weekStart, List<Schedule> schedules, List<GarbageCollectionEmployee> employees,
                        Predicate<LocalDate> badWeather) {
        this.weekStart = weekStart;
        this.schedules = schedules;
        this.employees = employees;
        this.badWeather = badWeather;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);
        int w = getWidth(), h = getHeight();
        int left = 48, top = 40, bottom = 22;
        int days = 6;
        double colW = (w - left - 4) / (double) days;
        int hours = 12;
        double rowH = (h - top - bottom) / (double) hours;
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        g.setFont(Theme.SMALL);
        for (int d = 0; d < days; d++) {
            LocalDate date = weekStart.plusDays(d);
            int x = (int) (left + d * colW);
            if (badWeather.test(date)) {
                g.setColor(new Color(0xFFEBEE));
                g.fillRect(x, top, (int) colW, h - top - bottom);
                g.setColor(Theme.RED);
                g.drawString("extreme weather", x + 6, 34);
            }
            if (date.equals(LocalDate.now())) {
                g.setColor(new Color(0xE8F5E9));
                g.fillRect(x, 0, (int) colW, top);
            }
            g.setColor(Theme.TEXT);
            g.setFont(Theme.BOLD);
            g.drawString(date.format(DateTimeFormatter.ofPattern("EEE dd MMM")), x + 6, 18);
            g.setFont(Theme.SMALL);
        }
        for (int i = 0; i <= hours; i++) {
            int y = (int) (top + i * rowH);
            g.setColor(new Color(0xECEFF1));
            g.drawLine(left, y, w, y);
            g.setColor(Theme.MUTED);
            if (i < hours) g.drawString(String.format("%02d:00", 6 + i), 6, y + 12);
        }
        for (int d = 0; d <= days; d++) {
            g.setColor(new Color(0xECEFF1));
            int x = (int) (left + d * colW);
            g.drawLine(x, top, x, h - bottom);
        }

        // count shifts that share a cell, so they can be drawn side by side
        for (Schedule s : schedules) {
            int day = (int) (s.getDate().toEpochDay() - weekStart.toEpochDay());
            if (day < 0 || day >= days) continue;
            List<Schedule> same = new ArrayList<>();
            for (Schedule o : schedules) if (o.overlapsWith(s)) same.add(o);
            int index = same.indexOf(s);
            double slotW = (colW - 6) / same.size();
            int x = (int) (left + day * colW + 3 + index * slotW);
            int y1 = timeY(s.getStartTime(), top, rowH);
            int y2 = timeY(s.getEndTime(), top, rowH);
            Color c = s.getEmployee() == null ? new Color(0x9E9E9E) : EMPLOYEE_COLORS[Math.max(0, employees.indexOf(s.getEmployee())) % EMPLOYEE_COLORS.length];
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
            g.fillRoundRect(x, y1 + 1, (int) slotW - 2, y2 - y1 - 2, 8, 8);
            Color border = s.getStatus() == Schedule.Status.NEEDS_MANAGER ? Theme.RED
                    : s.getStatus() == Schedule.Status.MOVED ? Theme.ORANGE : c;
            g.setColor(border);
            g.setStroke(new BasicStroke(s.getStatus() == Schedule.Status.NEEDS_MANAGER ? 2.5f : 1.5f));
            g.drawRoundRect(x, y1 + 1, (int) slotW - 2, y2 - y1 - 2, 8, 8);
            g.fillRect(x, y1 + 4, 4, y2 - y1 - 8);
            g.setClip(x, y1, (int) slotW - 3, y2 - y1);
            g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
            g.setColor(Theme.TEXT);
            g.drawString("#" + s.getScheduleNumber() + " " + s.getArea(), x + 7, y1 + 14);
            g.setFont(Theme.SMALL);
            g.setColor(Theme.MUTED);
            g.drawString(s.getEmployee() == null ? "not assigned" : s.getEmployee().getName(), x + 7, y1 + 27);
            if (s.getStatus() == Schedule.Status.NEEDS_MANAGER || s.getStatus() == Schedule.Status.MOVED) {
                g.setColor(border);
                g.drawString(s.getStatus() == Schedule.Status.MOVED ? "moved" : "decide!", x + 7, y1 + 40);
            }
            g.setClip(null);
        }

        // legend
        int lx = left;
        g.setFont(Theme.SMALL);
        for (int i = 0; i < employees.size(); i++) {
            g.setColor(EMPLOYEE_COLORS[i % EMPLOYEE_COLORS.length]);
            g.fillRect(lx, h - 10, 10, 8);
            g.setColor(Theme.TEXT);
            g.drawString(employees.get(i).getName(), lx + 13, h - 2);
            lx += 24 + g.getFontMetrics().stringWidth(employees.get(i).getName());
        }
        g.dispose();
    }

    private static int timeY(LocalTime t, int top, double rowH) {
        double hours = Math.max(0, Math.min(12, t.getHour() + t.getMinute() / 60.0 - 6));
        return (int) (top + hours * rowH);
    }
}
