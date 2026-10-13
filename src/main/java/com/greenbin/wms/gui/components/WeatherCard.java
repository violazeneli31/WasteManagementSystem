package com.greenbin.wms.gui.components;

import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.weather.WeatherData;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.time.format.TextStyle;
import java.util.Locale;

/** One day of the forecast with a drawn weather icon (FR_07). Red border = extreme weather. */
public class WeatherCard extends JComponent {
    private WeatherData data;

    public WeatherCard(WeatherData data) {
        this.data = data;
        setPreferredSize(new Dimension(82, 112));
    }

    public void setData(WeatherData data) { this.data = data; repaint(); }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);
        int w = getWidth(), h = getHeight();
        boolean severe = data.isSevere();
        g.setColor(severe ? new Color(0xFFEBEE) : Color.WHITE);
        g.fillRoundRect(1, 1, w - 3, h - 3, 12, 12);
        g.setColor(severe ? Theme.RED : Theme.BORDER);
        g.setStroke(new BasicStroke(severe ? 2f : 1f));
        g.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);

        g.setFont(Theme.BOLD);
        g.setColor(Theme.TEXT);
        String day = data.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + data.getDate().getDayOfMonth();
        g.drawString(day, w / 2 - g.getFontMetrics().stringWidth(day) / 2, 18);

        drawIcon(g, w / 2, 46, data.getCondition());

        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        g.setColor(severe ? Theme.RED : Theme.TEXT);
        String c = data.getCondition().getLabel();
        g.drawString(c, w / 2 - g.getFontMetrics().stringWidth(c) / 2, 82);
        g.setFont(Theme.SMALL);
        g.setColor(Theme.MUTED);
        String t = Math.round(data.getTemperature()) + "°C  " + Math.round(data.getWindSpeed()) + " km/h";
        g.drawString(t, w / 2 - g.getFontMetrics().stringWidth(t) / 2, 98);
        g.dispose();
    }

    private void drawIcon(Graphics2D g, int x, int y, WeatherData.Condition c) {
        switch (c) {
            case SUNNY:
            case HEAT_WAVE:
                g.setColor(c == WeatherData.Condition.HEAT_WAVE ? new Color(0xFF7043) : new Color(0xFFB300));
                g.setStroke(new BasicStroke(2f));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    g.drawLine((int) (x + 13 * Math.cos(a)), (int) (y + 13 * Math.sin(a)), (int) (x + 18 * Math.cos(a)), (int) (y + 18 * Math.sin(a)));
                }
                g.fillOval(x - 10, y - 10, 20, 20);
                break;
            default:
                cloud(g, x, y, c == WeatherData.Condition.STORM ? new Color(0x546E7A) : new Color(0xB0BEC5));
                if (c == WeatherData.Condition.RAIN || c == WeatherData.Condition.STORM) {
                    g.setColor(new Color(0x42A5F5));
                    g.setStroke(new BasicStroke(2f));
                    for (int i = -1; i <= 1; i++) g.drawLine(x + i * 8, y + 10, x + i * 8 - 3, y + 17);
                }
                if (c == WeatherData.Condition.STORM) {
                    g.setColor(new Color(0xFFD600));
                    g.fillPolygon(new Polygon(new int[]{x + 2, x - 5, x, x - 3, x + 6, x + 1}, new int[]{y + 2, y + 12, y + 12, y + 20, y + 9, y + 9}, 6));
                }
                if (c == WeatherData.Condition.HEAVY_SNOW) {
                    g.setColor(new Color(0x90CAF9));
                    for (int i = -1; i <= 1; i++) g.fillOval(x + i * 8 - 2, y + 12, 5, 5);
                }
        }
    }

    private void cloud(Graphics2D g, int x, int y, Color c) {
        g.setColor(c);
        g.fillOval(x - 16, y - 4, 18, 14);
        g.fillOval(x - 8, y - 12, 20, 20);
        g.fillOval(x + 2, y - 4, 16, 14);
        g.fillRect(x - 10, y + 2, 22, 8);
    }
}
