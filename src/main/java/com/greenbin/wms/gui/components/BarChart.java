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

/** A simple bar chart. Each bar can have a second, lighter bar next to it (e.g. "predicted"). */
public class BarChart extends JComponent {
    private List<String> labels = new ArrayList<>();
    private List<Double> values = new ArrayList<>();
    private List<Double> second = null;
    private List<Color> colors = new ArrayList<>();
    private String unit = "";
    private String firstName = "";
    private String secondName = "";

    public BarChart() { setPreferredSize(new Dimension(420, 240)); }

    public void setData(List<String> labels, List<Double> values, List<Color> colors, String unit) {
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        this.unit = unit;
        this.second = null;
        repaint();
    }

    /** Adds a second series drawn next to the first one, with a legend. */
    public void setSecondSeries(List<Double> second, String firstName, String secondName) {
        this.second = second;
        this.firstName = firstName;
        this.secondName = secondName;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);
        int w = getWidth(), h = getHeight();
        int left = 52, bottom = 34, top = 26, right = 10;
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (values.isEmpty()) { g.dispose(); return; }

        double max = 1;
        for (double v : values) max = Math.max(max, v);
        if (second != null) for (double v : second) max = Math.max(max, v);
        max = niceMax(Math.max(max, 4));

        int chartH = h - top - bottom;
        g.setFont(Theme.SMALL);
        for (int i = 0; i <= 4; i++) {
            int y = top + chartH - chartH * i / 4;
            g.setColor(new Color(0xECEFF1));
            g.drawLine(left, y, w - right, y);
            g.setColor(Theme.MUTED);
            String txt = format(max * i / 4);
            g.drawString(txt, left - 6 - g.getFontMetrics().stringWidth(txt), y + 4);
        }

        int n = values.size();
        double slot = (double) (w - left - right) / n;
        int barW = (int) Math.max(6, slot * (second != null ? 0.32 : 0.55));
        for (int i = 0; i < n; i++) {
            int cx = (int) (left + slot * i + slot / 2);
            Color c = colors.isEmpty() ? Theme.GREEN : colors.get(i % colors.size());
            int x1 = second != null ? cx - barW - 1 : cx - barW / 2;
            drawBar(g, x1, barW, values.get(i), max, top, chartH, c, true);
            if (second != null && i < second.size()) {
                drawBar(g, cx + 1, barW, second.get(i), max, top, chartH,
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), 110), false);
            }
            g.setColor(Theme.TEXT);
            String label = labels.get(i);
            while (g.getFontMetrics().stringWidth(label) > slot - 2 && label.length() > 3) label = label.substring(0, label.length() - 1);
            g.drawString(label, cx - g.getFontMetrics().stringWidth(label) / 2, h - bottom + 16);
        }
        g.setColor(Theme.MUTED);
        g.drawString(unit, 4, 12);
        if (second != null) {
            int x = w - 230;
            g.setColor(Theme.GREEN);
            g.fillRect(x, 6, 10, 10);
            g.setColor(Theme.TEXT);
            g.drawString(firstName, x + 14, 15);
            x += 24 + g.getFontMetrics().stringWidth(firstName);
            g.setColor(new Color(46, 125, 50, 110));
            g.fillRect(x, 6, 10, 10);
            g.setColor(Theme.TEXT);
            g.drawString(secondName, x + 14, 15);
        }
        g.dispose();
    }

    private void drawBar(Graphics2D g, int x, int barW, double v, double max, int top, int chartH, Color c, boolean label) {
        int bh = (int) Math.round(chartH * v / max);
        int y = top + chartH - bh;
        g.setColor(c);
        g.fillRoundRect(x, y, barW, bh, 6, 6);
        g.fillRect(x, y + Math.min(6, bh), barW, Math.max(0, bh - 6));
        if (!label) {
            g.setColor(c.darker());
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5f, new float[]{3f, 3f}, 0f));
            g.drawRect(x, y, barW, bh);
            g.setStroke(new BasicStroke(1f));
        }
        g.setColor(Theme.TEXT);
        String txt = format(v);
        if (g.getFontMetrics().stringWidth(txt) <= barW + 18) g.drawString(txt, x + barW / 2 - g.getFontMetrics().stringWidth(txt) / 2, y - 3);
    }

    static double niceMax(double v) {
        double p = Math.pow(10, Math.floor(Math.log10(v)));
        double[] steps = {1, 2, 2.5, 5, 10};
        for (double s : steps) if (s * p >= v) return s * p;
        return 10 * p;
    }

    static String format(double v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (Math.abs(v) >= 10_000) return String.format("%.0fk", v / 1000);
        if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
        return String.format("%.1f", v);
    }
}
