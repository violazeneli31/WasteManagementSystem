package com.greenbin.wms.gui.components;

import com.greenbin.wms.bins.DonationContainer;
import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.bins.SmartContainer;
import com.greenbin.wms.collection.LocationType;
import com.greenbin.wms.collection.Route;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.collection.WorkingZoneMap;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.system.GreenBinSystem;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The interactive city map (FR_01, FR_04, FR_06, FR_09, FR_11).
 * It draws the zone, the locations, the smart bins, the moving trucks and an optional route.
 * Clicking reports either the clicked object or the clicked coordinate.
 */
public class MapView extends JComponent {
    private static final int MARGIN = 24;

    private final GreenBinSystem sys;
    private boolean showBins = true;
    private boolean showVehicles = true;
    private boolean showLabels = true;
    private Route route;
    private Object selected;
    private Consumer<Object> onSelect;
    private Consumer<Coordinate> onEmptyClick;
    private Coordinate marker;

    public MapView(GreenBinSystem sys) {
        this.sys = sys;
        setPreferredSize(new Dimension(640, 480));
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object hit = findAt(e.getPoint());
                if (hit != null && onSelect != null) {
                    selected = hit;
                    onSelect.accept(hit);
                } else if (onEmptyClick != null) {
                    marker = toCoordinate(e.getPoint());
                    onEmptyClick.accept(marker);
                }
                repaint();
            }
        });
    }

    // ---------- settings ----------

    public void setRoute(Route route) { this.route = route; repaint(); }
    public void setShowBins(boolean show) { this.showBins = show; repaint(); }
    public void setShowVehicles(boolean show) { this.showVehicles = show; repaint(); }
    public void setShowLabels(boolean show) { this.showLabels = show; repaint(); }
    public void setSelected(Object o) { this.selected = o; repaint(); }
    public Object getSelected() { return selected; }
    public void setMarker(Coordinate c) { this.marker = c; repaint(); }
    public void onSelect(Consumer<Object> listener) { this.onSelect = listener; }
    public void onEmptyClick(Consumer<Coordinate> listener) { this.onEmptyClick = listener; }

    // ---------- coordinates <-> pixels ----------

    private double kmPerDegLon() { return 111.0 * Math.cos(Math.toRadians(41.33)); }

    private double scale() {
        WorkingZoneMap z = sys.getZoneMap();
        double wKm = (z.getMaxLon() - z.getMinLon()) * kmPerDegLon();
        double hKm = (z.getMaxLat() - z.getMinLat()) * 111.0;
        return Math.min((getWidth() - 2.0 * MARGIN) / wKm, (getHeight() - 2.0 * MARGIN) / hKm);
    }

    private double[] offset() {
        WorkingZoneMap z = sys.getZoneMap();
        double s = scale();
        double wPx = (z.getMaxLon() - z.getMinLon()) * kmPerDegLon() * s;
        double hPx = (z.getMaxLat() - z.getMinLat()) * 111.0 * s;
        return new double[]{(getWidth() - wPx) / 2, (getHeight() - hPx) / 2};
    }

    public Point toPoint(Coordinate c) {
        WorkingZoneMap z = sys.getZoneMap();
        double s = scale();
        double[] o = offset();
        int x = (int) Math.round(o[0] + (c.getLongitude() - z.getMinLon()) * kmPerDegLon() * s);
        int y = (int) Math.round(o[1] + (z.getMaxLat() - c.getLatitude()) * 111.0 * s);
        return new Point(x, y);
    }

    public Coordinate toCoordinate(Point p) {
        WorkingZoneMap z = sys.getZoneMap();
        double s = scale();
        double[] o = offset();
        double lon = z.getMinLon() + (p.x - o[0]) / (kmPerDegLon() * s);
        double lat = z.getMaxLat() - (p.y - o[1]) / (111.0 * s);
        return new Coordinate(Math.max(-90, Math.min(90, lat)), Math.max(-180, Math.min(180, lon)));
    }

    // ---------- clicking & tooltips ----------

    private Object findAt(Point p) {
        Object best = null;
        double bestDist = 14;
        List<Object> objects = new ArrayList<>();
        if (showVehicles) objects.addAll(sys.getVehicles());
        if (showBins) objects.addAll(sys.getContainers());
        objects.addAll(sys.getZoneMap().getLocations());
        for (Object o : objects) {
            double d = toPoint(coordinateOf(o)).distance(p);
            if (d < bestDist) { bestDist = d; best = o; }
        }
        return best;
    }

    private static Coordinate coordinateOf(Object o) {
        if (o instanceof Vehicle) return ((Vehicle) o).getCurrentLocation();
        if (o instanceof SmartContainer) return ((SmartContainer) o).getCoordinate();
        return ((ZoneLocation) o).getCoordinate();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Object o = findAt(e.getPoint());
        if (o instanceof Vehicle) {
            Vehicle v = (Vehicle) o;
            return "<html><b>Truck " + v.getPlateNumber() + "</b><br>" + v.getStatusText() + "<br>GPS: "
                    + v.getCurrentLocation() + "<br>Fuel: " + Math.round(v.getFuelLevel()) + "%</html>";
        }
        if (o instanceof SmartBin) {
            SmartBin b = (SmartBin) o;
            String fill = b.getSensor().isWorking() ? Math.round(b.getFillPercent()) + "% (" + b.getStatus() + ")" : "SENSOR BROKEN";
            return "<html><b>" + b.getWasteType() + " bin</b> - " + b.getLocation() + "<br>Fill: " + fill
                    + "<br>Barcode: " + b.getBarcode() + "</html>";
        }
        if (o instanceof DonationContainer) {
            DonationContainer d = (DonationContainer) o;
            return "<html><b>Donation container</b> - " + d.getLocation() + "<br>" + d.getDonations().size() + " items, "
                    + Math.round(d.getFillPercent()) + "% full</html>";
        }
        if (o instanceof ZoneLocation) {
            ZoneLocation l = (ZoneLocation) o;
            return "<html><b>" + l.getName() + "</b><br>" + l.getType() + "<br>" + l.getCoordinate()
                    + (sys.getBlockedLocations().contains(l) ? "<br><font color=red>Road blocked</font>" : "") + "</html>";
        }
        return toCoordinate(e.getPoint()).toString();
    }

    // ---------- drawing ----------

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);
        g.setColor(new Color(0xF7F9F7));
        g.fillRect(0, 0, getWidth(), getHeight());
        drawCity(g);
        if (route != null) drawRoute(g);
        drawLocations(g);
        if (showBins) drawContainers(g);
        if (showVehicles) drawVehicles(g);
        if (marker != null) drawMarker(g, marker);
        drawLegend(g);
        g.dispose();
    }

    private void drawCity(Graphics2D g) {
        WorkingZoneMap z = sys.getZoneMap();
        Point tl = toPoint(new Coordinate(z.getMaxLat(), z.getMinLon()));
        Point br = toPoint(new Coordinate(z.getMinLat(), z.getMaxLon()));
        g.setColor(new Color(0xEAF0EA));
        g.fillRect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);

        // small streets
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        for (double lat = z.getMinLat(); lat <= z.getMaxLat(); lat += 0.004) line(g, lat, z.getMinLon(), lat, z.getMaxLon());
        for (double lon = z.getMinLon(); lon <= z.getMaxLon(); lon += 0.005) line(g, z.getMinLat(), lon, z.getMaxLat(), lon);

        // Grand Park and the lake
        g.setColor(new Color(0xC8E6C9));
        ellipse(g, 41.3125, 19.8205, 0.0095, 0.0150);
        g.setColor(new Color(0xB3E5FC));
        ellipse(g, 41.3065, 19.8190, 0.0035, 0.0080);
        g.setColor(new Color(0x81A784));
        g.setFont(Theme.SMALL);
        Point park = toPoint(new Coordinate(41.3135, 19.8150));
        g.drawString("Grand Park", park.x, park.y);

        // the Lana river
        g.setColor(new Color(0x90CAF9));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double[][] river = {{41.3230, 19.7700}, {41.3215, 19.7900}, {41.3205, 19.8050}, {41.3200, 19.8200}, {41.3212, 19.8350}, {41.3240, 19.8600}};
        Path2D path = new Path2D.Double();
        for (int i = 0; i < river.length; i++) {
            Point p = toPoint(new Coordinate(river[i][0], river[i][1]));
            if (i == 0) path.moveTo(p.x, p.y); else path.lineTo(p.x, p.y);
        }
        g.draw(path);

        // main roads
        g.setColor(new Color(0xFFF3E0));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        line(g, 41.3060, 19.8185, 41.3420, 19.8185);
        line(g, 41.3275, 19.7720, 41.3275, 19.8580);
        line(g, 41.3420, 19.7900, 41.3180, 19.8450);
        g.setColor(new Color(0xFFCC80));
        g.setStroke(new BasicStroke(1f));
        line(g, 41.3060, 19.8185, 41.3420, 19.8185);
        line(g, 41.3275, 19.7720, 41.3275, 19.8580);
        line(g, 41.3420, 19.7900, 41.3180, 19.8450);

        // zone border
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f));
        g.drawRect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);
        g.setFont(Theme.BOLD);
        g.drawString("Zone: " + z.getZoneName(), tl.x + 8, tl.y + 18);
    }

    private void line(Graphics2D g, double lat1, double lon1, double lat2, double lon2) {
        Point a = toPoint(new Coordinate(lat1, lon1));
        Point b = toPoint(new Coordinate(lat2, lon2));
        g.drawLine(a.x, a.y, b.x, b.y);
    }

    private void ellipse(Graphics2D g, double lat, double lon, double dLat, double dLon) {
        Point a = toPoint(new Coordinate(lat + dLat / 2, lon - dLon / 2));
        Point b = toPoint(new Coordinate(lat - dLat / 2, lon + dLon / 2));
        g.fillOval(a.x, a.y, b.x - a.x, b.y - a.y);
    }

    private void drawRoute(Graphics2D g) {
        List<Coordinate> pts = route.getOptimizedRoute();
        g.setColor(new Color(21, 101, 192, 170));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < pts.size() - 1; i++) {
            Point a = toPoint(pts.get(i));
            Point b = toPoint(pts.get(i + 1));
            g.drawLine(a.x, a.y, b.x, b.y);
            drawArrow(g, a, b);
        }
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        for (int i = 0; i < route.getOrderedStops().size(); i++) {
            Point p = toPoint(route.getOrderedStops().get(i).getCoordinate());
            g.setColor(Theme.BLUE);
            g.fillOval(p.x + 6, p.y - 22, 18, 18);
            g.setColor(Color.WHITE);
            String n = String.valueOf(i + 1);
            g.drawString(n, p.x + 15 - g.getFontMetrics().stringWidth(n) / 2, p.y - 9);
        }
        for (ZoneLocation skipped : route.getSkippedStops()) {
            Point p = toPoint(skipped.getCoordinate());
            g.setColor(Theme.RED);
            g.setStroke(new BasicStroke(3f));
            g.drawLine(p.x - 9, p.y - 9, p.x + 9, p.y + 9);
            g.drawLine(p.x - 9, p.y + 9, p.x + 9, p.y - 9);
        }
    }

    private void drawArrow(Graphics2D g, Point a, Point b) {
        double angle = Math.atan2(b.y - a.y, b.x - a.x);
        int mx = (a.x + b.x) / 2;
        int my = (a.y + b.y) / 2;
        Polygon head = new Polygon();
        head.addPoint((int) (mx + 8 * Math.cos(angle)), (int) (my + 8 * Math.sin(angle)));
        head.addPoint((int) (mx + 7 * Math.cos(angle + 2.5)), (int) (my + 7 * Math.sin(angle + 2.5)));
        head.addPoint((int) (mx + 7 * Math.cos(angle - 2.5)), (int) (my + 7 * Math.sin(angle - 2.5)));
        g.fillPolygon(head);
    }

    private void drawLocations(Graphics2D g) {
        g.setFont(Theme.SMALL);
        for (ZoneLocation l : sys.getZoneMap().getLocations()) {
            Point p = toPoint(l.getCoordinate());
            boolean isSelected = l == selected;
            if (isSelected) {
                g.setColor(new Color(255, 193, 7, 120));
                g.fillOval(p.x - 16, p.y - 16, 32, 32);
            }
            g.setStroke(new BasicStroke(2f));
            switch (l.getType()) {
                case INSTITUTION:
                    g.setColor(Theme.BLUE);
                    g.fillRect(p.x - 7, p.y - 7, 14, 14);
                    g.setColor(Color.WHITE);
                    g.drawString("i", p.x - 2, p.y + 4);
                    break;
                case RECYCLING_FACILITY:
                    g.setColor(Theme.PURPLE);
                    Polygon tri = new Polygon(new int[]{p.x, p.x - 9, p.x + 9}, new int[]{p.y - 10, p.y + 7, p.y + 7}, 3);
                    g.fillPolygon(tri);
                    break;
                case DEPOT:
                    g.setColor(new Color(0x37474F));
                    g.fillRoundRect(p.x - 9, p.y - 9, 18, 18, 5, 5);
                    g.setColor(Color.WHITE);
                    g.drawString("D", p.x - 4, p.y + 4);
                    break;
                default:
                    g.setColor(Color.WHITE);
                    g.fillOval(p.x - 7, p.y - 7, 14, 14);
                    g.setColor(Theme.GREEN);
                    g.drawOval(p.x - 7, p.y - 7, 14, 14);
                    g.fillOval(p.x - 3, p.y - 3, 6, 6);
            }
            if (sys.getBlockedLocations().contains(l)) {
                g.setColor(Theme.RED);
                g.setStroke(new BasicStroke(3f));
                g.drawLine(p.x - 10, p.y - 10, p.x + 10, p.y + 10);
                g.drawLine(p.x - 10, p.y + 10, p.x + 10, p.y - 10);
            }
            if (showLabels) {
                g.setColor(Theme.TEXT);
                g.drawString(l.getName(), p.x + 10, p.y + 4);
            }
        }
    }

    private void drawContainers(Graphics2D g) {
        for (SmartContainer c : sys.getContainers()) {
            Point p = toPoint(c.getCoordinate());
            int x = p.x - 5, y = p.y + 6;
            if (c == selected) {
                g.setColor(new Color(255, 193, 7, 140));
                g.fillOval(x - 8, y - 6, 26, 26);
            }
            Color body = c instanceof SmartBin ? Theme.colorOf(((SmartBin) c).getWasteType()) : new Color(0xAD1457);
            g.setColor(body);
            g.fillRoundRect(x, y, 10, 14, 3, 3);
            if (!c.getSensor().isWorking()) {
                g.setColor(Color.WHITE);
                g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
                g.drawString("?", x + 2, y + 11);
            } else {
                int h = (int) Math.round(12 * c.getFillPercent() / 100);
                g.setColor(Theme.fillColor(c.getFillPercent()));
                g.fillRect(x + 12, y + 14 - h, 4, h);
            }
            if (c.checkIfFull()) {
                g.setColor(Theme.RED);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(x - 2, y - 2, 14, 18, 4, 4);
            }
        }
    }

    private void drawVehicles(Graphics2D g) {
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        for (Vehicle v : sys.getVehicles()) {
            Point p = toPoint(v.getCurrentLocation());
            if (v == selected) {
                g.setColor(new Color(255, 152, 0, 90));
                g.fillOval(p.x - 20, p.y - 20, 40, 40);
            }
            g.setColor(new Color(0xF57C00));
            g.fillRoundRect(p.x - 11, p.y - 6, 15, 12, 3, 3);       // container
            g.setColor(new Color(0xE65100));
            g.fillRoundRect(p.x + 4, p.y - 3, 8, 9, 3, 3);          // cab
            g.setColor(new Color(0x263238));
            g.fillOval(p.x - 9, p.y + 4, 6, 6);
            g.fillOval(p.x + 5, p.y + 4, 6, 6);
            String label = v.getPlateNumber();
            int w = g.getFontMetrics().stringWidth(label);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillRoundRect(p.x - w / 2 - 3, p.y - 22, w + 6, 14, 6, 6);
            g.setColor(new Color(0xBF360C));
            g.drawString(label, p.x - w / 2, p.y - 11);
        }
    }

    private void drawMarker(Graphics2D g, Coordinate c) {
        Point p = toPoint(c);
        g.setColor(Theme.RED);
        g.fillOval(p.x - 6, p.y - 18, 12, 12);
        Polygon pin = new Polygon(new int[]{p.x - 5, p.x + 5, p.x}, new int[]{p.y - 9, p.y - 9, p.y}, 3);
        g.fillPolygon(pin);
        g.setColor(Color.WHITE);
        g.fillOval(p.x - 2, p.y - 14, 4, 4);
    }

    private void drawLegend(Graphics2D g) {
        int x = 10, y = getHeight() - 26;
        g.setColor(new Color(255, 255, 255, 225));
        g.fillRoundRect(x - 4, y - 14, Math.min(getWidth() - 12, 560), 26, 10, 10);
        g.setFont(Theme.SMALL);
        x = legendItem(g, x, y, Theme.GREEN, "Pickup point", 0);
        x = legendItem(g, x, y, Theme.BLUE, "Institution", 1);
        x = legendItem(g, x, y, Theme.PURPLE, "Recycling", 2);
        x = legendItem(g, x, y, new Color(0x37474F), "Depot", 1);
        x = legendItem(g, x, y, Theme.colorOf(com.greenbin.wms.model.WasteType.PAPER), "Smart bin", 1);
        x = legendItem(g, x, y, new Color(0xAD1457), "Donations", 1);
        legendItem(g, x, y, new Color(0xF57C00), "Truck (live)", 1);
    }

    private int legendItem(Graphics2D g, int x, int y, Color c, String text, int shape) {
        g.setColor(c);
        if (shape == 0) g.fillOval(x, y - 9, 10, 10);
        else if (shape == 2) g.fillPolygon(new int[]{x + 5, x, x + 10}, new int[]{y - 10, y, y}, 3);
        else g.fillRect(x, y - 9, 10, 10);
        g.setColor(Theme.TEXT);
        g.drawString(text, x + 14, y);
        return x + 22 + g.getFontMetrics().stringWidth(text);
    }

    /** Draws a location type icon (used in lists). */
    public static Color colorOf(LocationType t) {
        switch (t) {
            case INSTITUTION: return Theme.BLUE;
            case RECYCLING_FACILITY: return Theme.PURPLE;
            case DEPOT: return new Color(0x37474F);
            default: return Theme.GREEN;
        }
    }
}
