package com.greenbin.wms.gui.screens;

import com.greenbin.wms.collection.LocationType;
import com.greenbin.wms.collection.Route;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.MapView;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.GarbageCollectionEmployee;
import com.greenbin.wms.routing.IRouteOptimizer;
import com.greenbin.wms.routing.NormalRoutineRoute;
import com.greenbin.wms.routing.RouteConditions;
import com.greenbin.wms.routing.RouteOptimizationCenter;
import com.greenbin.wms.routing.SpecialPickupsRoute;
import com.greenbin.wms.routing.WeatherBasedRoute;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** FR_04 route optimization with three strategies, chosen by the manager or automatically. */
public class RouteScreen extends Screen {
    private static final String AUTO = "Automatic - the system chooses";

    private final MapView map;
    private final List<ZoneLocation> stops = new ArrayList<>();
    private final DefaultTableModel stopModel = new DefaultTableModel(new String[]{"Visit", "Stop", "Special pickup", "Road blocked"}, 0) {
        @Override public Class<?> getColumnClass(int c) { return c == 1 ? String.class : Boolean.class; }
        @Override public boolean isCellEditable(int r, int c) { return c != 1; }
    };
    private final JComboBox<Object> strategy = new JComboBox<>(new Object[]{AUTO, new NormalRoutineRoute(), new WeatherBasedRoute(), new SpecialPickupsRoute()});
    private final JComboBox<Vehicle> truck = new JComboBox<>();
    private final JComboBox<GarbageCollectionEmployee> driver = new JComboBox<>();
    private final JTextArea result = new JTextArea(7, 26);
    private final DefaultTableModel compare = Tables.model("Route type", "Stops", "Distance", "Time");
    private Route route;
    private boolean loading;

    public RouteScreen(Account me) {
        super(me, "Route Optimization", "Choose the stops and conditions, the system finds the shortest order (closest stop next)",
                "FR_04", "FR_06");
        map = new MapView(sys);
        body().add(Theme.card(null, map), BorderLayout.CENTER);

        strategy.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v instanceof IRouteOptimizer ? ((IRouteOptimizer) v).getName() : v, i, s, f);
            }
        });
        JTable stopTable = new JTable(stopModel);
        stopTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        stopTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        stopTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        stopTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        stopModel.addTableModelListener(e -> {
            if (loading || e.getColumn() != 3) return;
            int r = e.getFirstRow();
            sys.setBlocked(stops.get(r), (Boolean) stopModel.getValueAt(r, 3));
            map.repaint();
        });
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(340, 100));
        JPanel stopsCard = Theme.card("1. Stops for today", Theme.table(stopTable));
        left.add(stopsCard, BorderLayout.CENTER);

        JPanel options = new Stack();
        options.setOpaque(false);
        options.add(muted("Route type"));
        options.add(strategy);
        options.add(row(muted("Truck"), truck));
        options.add(row(muted("Driver"), driver));
        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        buttons.setOpaque(false);
        buttons.add(FlatButton.primary("Optimize", e -> optimize()));
        buttons.add(FlatButton.primary("Confirm & send", e -> confirmRoute()));
        buttons.add(FlatButton.secondary("Nearest truck", e -> nearestTruck()));
        options.add(buttons);
        left.add(Theme.card("2. Conditions", options), BorderLayout.SOUTH);
        body().add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(290, 100));
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setFont(Theme.NORMAL);
        result.setText("Press Optimize to see the route.");
        right.add(Theme.card("3. Result", result), BorderLayout.CENTER);
        JPanel cmp = Theme.card("Same stops, every route type", Theme.table(Theme.newTable(compare)));
        cmp.setPreferredSize(new Dimension(300, 190));
        right.add(cmp, BorderLayout.SOUTH);
        body().add(right, BorderLayout.EAST);
    }

    private RouteConditions conditions(List<ZoneLocation> chosen) {
        RouteConditions c = new RouteConditions();
        for (int r = 0; r < stops.size(); r++) {
            if ((Boolean) stopModel.getValueAt(r, 2) && chosen.contains(stops.get(r))) c.getPriority().add(stops.get(r));
        }
        return c;
    }

    private List<ZoneLocation> chosenStops() {
        List<ZoneLocation> chosen = new ArrayList<>();
        for (int r = 0; r < stops.size(); r++) if ((Boolean) stopModel.getValueAt(r, 0)) chosen.add(stops.get(r));
        return chosen;
    }

    private void optimize() {
        List<ZoneLocation> chosen = chosenStops();
        if (chosen.isEmpty()) {
            error("Tick at least one stop to visit.");
            return;
        }
        Object s = strategy.getSelectedItem();
        route = sys.buildRoute(LocalDate.now(), chosen, conditions(chosen), s instanceof IRouteOptimizer ? (IRouteOptimizer) s : null);
        map.setRoute(route);
        StringBuilder sb = new StringBuilder();
        sb.append("Route type: ").append(route.getStrategyName()).append('\n');
        sb.append(RouteOptimizationCenter.getInstance().getStrategy().getDescription()).append("\n\n");
        sb.append(String.format("Distance: %.2f km%nTime: about %d min (%d stops)%n%n", route.getTotalDistanceKm(),
                route.getEstimatedTime().toMinutes(), route.getOrderedStops().size()));
        for (int i = 0; i < route.getOrderedStops().size(); i++) sb.append(i + 1).append(". ").append(route.getOrderedStops().get(i)).append('\n');
        if (!route.getSkippedStops().isEmpty()) sb.append("\nSkipped (road blocked): ").append(route.getSkippedStops());
        result.setText(sb.toString());
        result.setCaretPosition(0);

        compare.setRowCount(0);
        for (IRouteOptimizer o : new IRouteOptimizer[]{new NormalRoutineRoute(), new WeatherBasedRoute(), new SpecialPickupsRoute()}) {
            Route r = sys.buildRoute(LocalDate.now(), chosen, conditions(chosen), o);
            compare.addRow(new Object[]{o.getName(), r.getOrderedStops().size(), String.format("%.2f km", r.getTotalDistanceKm()),
                    r.getEstimatedTime().toMinutes() + " min"});
        }
        // the compare loop changed the strategy; put back the one used for the shown route
        if (s instanceof IRouteOptimizer) RouteOptimizationCenter.getInstance().setStrategy((IRouteOptimizer) s);
    }

    private void confirmRoute() {
        if (route == null) {
            error("Optimize a route first.");
            return;
        }
        Vehicle v = (Vehicle) truck.getSelectedItem();
        GarbageCollectionEmployee d = (GarbageCollectionEmployee) driver.getSelectedItem();
        sys.confirmRoute(route, v, d);
        info("Route confirmed. Truck " + v + " is now driving it (watch the map) and " + d + " was notified.");
    }

    private void nearestTruck() {
        int r = -1;
        for (int i = 0; i < stops.size(); i++) if ((Boolean) stopModel.getValueAt(i, 2)) { r = i; break; }
        if (r < 0) {
            error("Tick 'Special pickup' for the place of the special request.");
            return;
        }
        ZoneLocation target = stops.get(r);
        Vehicle v = RouteOptimizationCenter.getInstance().findNearestVehicle(target.getCoordinate(), sys.getVehicles());
        map.setSelected(v);
        info(String.format("Closest truck to %s: %s (%.2f km away). It is highlighted on the map.", target, v,
                v.getCurrentLocation().distanceKm(target.getCoordinate())));
    }

    @Override
    public void refresh() {
        loading = true;
        stops.clear();
        stopModel.setRowCount(0);
        for (ZoneLocation l : sys.getZoneMap().getLocations()) {
            if (l.getType() == LocationType.DEPOT) continue;
            stops.add(l);
            boolean visit = l.getType() == LocationType.PICKUP_POINT;
            stopModel.addRow(new Object[]{visit, l.getName(), false, sys.getBlockedLocations().contains(l)});
        }
        loading = false;
        truck.removeAllItems();
        for (Vehicle v : sys.getVehicles()) truck.addItem(v);
        driver.removeAllItems();
        for (GarbageCollectionEmployee e : sys.getCollectors()) driver.addItem(e);
        if (route == null) optimize();
    }

    @Override
    public void onTick() { map.repaint(); }
}
