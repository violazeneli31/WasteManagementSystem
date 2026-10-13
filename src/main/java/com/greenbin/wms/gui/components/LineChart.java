package com.greenbin.wms.gui.components;

import com.greenbin.wms.gui.Theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** A line chart. The part of a line after "forecastFrom" is drawn dashed (predictions). */
public class LineChart extends JComponent {
    /** One line of the chart. */
    public static class Series {
        final String name;
        final Color color;
        final List<Double> values;

        public Series(String name, Color color, List<Double> values) {
            this.name = name;
            this.color = color;
            this.values = values;
        }
    }

    private final List<Series> series = new ArrayList<>();
    private List<String> xLabels = new ArrayList<>();
    private int forecastFrom = -1;
    private String unit = "";

    public LineChart() { setPreferredSize(new Dimension(460, 240)); }

    public void setData(List<String> xLabels, List<Series> data, int forecastFrom, String unit) {
        this.xLabels = xLabels;
        this.series.clear();
        this.series.addAll(data);
        this.forecastFrom = forecastFrom;
        this.unit = unit;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);
        int w = getWidth(), h = getHeight();
        int left = 52, right = 12, top = 26, bottom = 30;
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (series.isEmpty() || xLabels.isEmpty()) { g.dispose(); return; }

        double max = 1, min = Double.MAX_VALUE;
        for (Series s : series) for (double v : s.values) { max = Math.max(max, v); min = Math.min(min, v); }
        min = Math.max(0, min * 0.85);
        max = max * 1.05;
        int chartW = w - left - right, chartH = h - top - bottom;
        int n = xLabels.size();

        g.setFont(Theme.SMALL);
        for (int i = 0; i <= 4; i++) {
            int y = top + chartH - chartH * i / 4;
            g.setColor(new Color(0xECEFF1));
            g.drawLine(left, y, w - right, y);
            g.setColor(Theme.MUTED);
            String txt = BarChart.format(min + (max - min) * i / 4);
            g.drawString(txt, left - 6 - g.getFontMetrics().stringWidth(txt), y + 4);
        }
        if (forecastFrom > 0 && forecastFrom < n) {
            int fx = left + chartW * forecastFrom / Math.max(1, n - 1);
            g.setColor(new Color(0xFFF8E1));
            g.fillRect(fx, top, w - right - fx, chartH);
            g.setColor(new Color(0xF9A825));
            g.drawString("forecast", fx + 4, top + 12);
        }
        int step = Math.max(1, n / 8);
        g.setColor(Theme.MUTED);
        for (int i = 0; i < n; i += step) {
            int x = left + chartW * i / Math.max(1, n - 1);
            g.drawString(xLabels.get(i), x - g.getFontMetrics().stringWidth(xLabels.get(i)) / 2, h - 10);
        }

        int legendX = left;
        for (Series s : series) {
            for (int i = 0; i < s.values.size() - 1 && i < n - 1; i++) {
                boolean dashed = forecastFrom >= 0 && i >= forecastFrom - 1;
                g.setStroke(dashed ? new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5f, new float[]{6f, 5f}, 0f)
                        : new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(s.color);
                g.drawLine(x(i, left, chartW, n), y(s.values.get(i), min, max, top, chartH),
                        x(i + 1, left, chartW, n), y(s.values.get(i + 1), min, max, top, chartH));
            }
            g.setColor(s.color);
            g.fillRect(legendX, 8, 12, 4);
            g.setColor(Theme.TEXT);
            g.drawString(s.name, legendX + 16, 14);
            legendX += 30 + g.getFontMetrics().stringWidth(s.name);
        }
        g.setColor(Theme.MUTED);
        g.drawString(unit, w - right - g.getFontMetrics().stringWidth(unit), 14);
        g.dispose();
    }

    private static int x(int i, int left, int chartW, int n) { return left + chartW * i / Math.max(1, n - 1); }

    private static int y(double v, double min, double max, int top, int chartH) {
        return top + chartH - (int) Math.round(chartH * (v - min) / (max - min));
    }
}
