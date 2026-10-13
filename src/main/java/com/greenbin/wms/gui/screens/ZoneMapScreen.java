package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.SmartContainer;
import com.greenbin.wms.collection.BinPlacementOrder;
import com.greenbin.wms.collection.LocationType;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.MapView;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Account;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.EnumSet;
import java.util.Set;

/** FR_01 working zone map, FR_06 live vehicle tracking, FR_11 ordering bins at a location. */
public class ZoneMapScreen extends Screen {
    private final MapView map;
    private final JTextField name = new JTextField(14);
    private final JComboBox<LocationType> type = new JComboBox<>(LocationType.values());
    private final JCheckBox[] categories = new JCheckBox[WasteType.values().length];
    private final JLabel clicked = Theme.label("Click an empty spot on the map", Theme.SMALL, Theme.MUTED);
    private final JTextArea details = new JTextArea(5, 24);
    private final JComboBox<WasteType> binType = new JComboBox<>(WasteType.values());
    private final DefaultTableModel trucks = Tables.model("Truck", "Status", "GPS position", "Fuel");
    private final JTable truckTable = Theme.newTable(trucks);
    private Coordinate clickedPoint;

    public ZoneMapScreen(Account me) {
        super(me, "Working Zone Map", "Click the map to add a location, click a marker to select it. Trucks are tracked live.",
                "FR_01", "FR_06", "FR_11");
        map = new MapView(sys);
        map.onEmptyClick(c -> {
            clickedPoint = c;
            clicked.setText("Selected point: " + c);
            map.setSelected(null);
        });
        map.onSelect(o -> showDetails(o));
        body().add(Theme.card(null, map), BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(360, 100));

        // add a location
        JPanel form = new Stack();
        form.setOpaque(false);
        form.add(muted("Name"));
        form.add(name);
        form.add(muted("Type"));
        form.add(type);
        form.add(muted("Waste categories collected here"));
        JPanel cats = new JPanel(new GridLayout(0, 3));
        cats.setOpaque(false);
        for (int i = 0; i < categories.length; i++) {
            categories[i] = new JCheckBox(WasteType.values()[i].getLabel(), true);
            categories[i].setOpaque(false);
            categories[i].setFont(Theme.SMALL);
            cats.add(categories[i]);
        }
        form.add(cats);
        form.add(clicked);
        form.add(FlatButton.primary("Add location", e -> addLocation()));
        side.add(Theme.card("Add a location", form));

        // the selected object
        details.setEditable(false);
        details.setFont(Theme.NORMAL);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setText("Nothing selected.");
        JPanel sel = new JPanel(new BorderLayout(0, 6));
        sel.setOpaque(false);
        sel.add(details, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 2, 6, 6));
        actions.setOpaque(false);
        actions.add(FlatButton.danger("Remove location", e -> removeSelected()));
        actions.add(FlatButton.secondary("Block / open road", e -> toggleBlocked()));
        actions.add(binType);
        actions.add(FlatButton.primary("Order this bin here", e -> orderBin()));
        sel.add(actions, BorderLayout.SOUTH);
        side.add(Theme.card("Selected", sel));

        // trucks
        truckTable.getSelectionModel().addListSelectionListener(e -> {
            int r = truckTable.getSelectedRow();
            if (r >= 0 && r < sys.getVehicles().size()) map.setSelected(sys.getVehicles().get(r));
        });
        JPanel tcard = Theme.card("Trucks - live GPS (FR_06)", Theme.table(truckTable));
        tcard.setPreferredSize(new Dimension(360, 170));
        side.add(tcard);
        for (Component c : side.getComponents()) ((JPanel) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        body().add(side, BorderLayout.EAST);
    }

    private void addLocation() {
        if (clickedPoint == null) {
            error("First click on the map where the new location is.");
            return;
        }
        Set<WasteType> chosen = EnumSet.noneOf(WasteType.class);
        for (int i = 0; i < categories.length; i++) if (categories[i].isSelected()) chosen.add(WasteType.values()[i]);
        String problem = sys.addLocation(name.getText(), (LocationType) type.getSelectedItem(), clickedPoint, chosen);
        if (problem != null) {
            error(problem);
            return;
        }
        info("'" + name.getText().trim() + "' was added. Routes will include it from now on.");
        name.setText("");
        clickedPoint = null;
        map.setMarker(null);
        clicked.setText("Click an empty spot on the map");
    }

    private void showDetails(Object o) {
        if (o instanceof ZoneLocation) {
            ZoneLocation l = (ZoneLocation) o;
            details.setText(l.getName() + "\nType: " + l.getType() + "\nCoordinates: " + l.getCoordinate()
                    + "\nWaste: " + l.getWasteCategories()
                    + (sys.getBlockedLocations().contains(l) ? "\nROAD BLOCKED" : ""));
        } else if (o instanceof Vehicle) {
            Vehicle v = (Vehicle) o;
            details.setText("Truck " + v.getPlateNumber() + "\n" + v.getStatusText() + "\nGPS: " + v.getCurrentLocation()
                    + "\nCrew: " + v.getAssignedEmployees());
        } else if (o instanceof SmartContainer) {
            SmartContainer c = (SmartContainer) o;
            details.setText(c.getKind() + " at " + c.getLocation() + "\nFill: "
                    + (c.getSensor().isWorking() ? Math.round(c.getFillPercent()) + "%" : "sensor broken")
                    + "\nBarcode: " + c.getBarcode());
        }
    }

    private ZoneLocation selectedLocation() {
        Object o = map.getSelected();
        if (o instanceof ZoneLocation) return (ZoneLocation) o;
        error("Select a location on the map first (click a marker).");
        return null;
    }

    private void removeSelected() {
        ZoneLocation l = selectedLocation();
        if (l == null || !confirm("Remove '" + l.getName() + "' from the map?")) return;
        sys.removeLocation(l);
        map.setSelected(null);
        details.setText("Nothing selected.");
    }

    private void toggleBlocked() {
        ZoneLocation l = selectedLocation();
        if (l == null) return;
        sys.setBlocked(l, !sys.getBlockedLocations().contains(l));
        showDetails(l);
        map.repaint();
    }

    private void orderBin() {
        ZoneLocation l = selectedLocation();
        if (l == null) return;
        WasteType t = (WasteType) binType.getSelectedItem();
        BinPlacementOrder order = sys.orderBinPlacement(l, t);
        if (order == null) error("There are no " + t + " bins in the warehouse. (Stock: see Smart Bins > Inventory)");
        else info(order + " sent to " + order.getAssignedTo() + ".\nThey will confirm when the bin is placed.");
    }

    @Override
    public void refresh() { onTick(); }

    @Override
    public void onTick() {
        int sel = truckTable.getSelectedRow();
        trucks.setRowCount(0);
        for (Vehicle v : sys.getVehicles()) {
            trucks.addRow(new Object[]{v.getPlateNumber(), v.getStatusText(), v.getCurrentLocation().toString(),
                    Math.round(v.getFuelLevel()) + "%"});
        }
        if (sel >= 0 && sel < trucks.getRowCount()) truckTable.setRowSelectionInterval(sel, sel);
        map.repaint();
    }
}
