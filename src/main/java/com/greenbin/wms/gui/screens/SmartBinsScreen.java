package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.bins.SmartContainer;
import com.greenbin.wms.bins.WasteReport;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.gui.components.BinGraphic;
import com.greenbin.wms.gui.components.CodeViews;
import com.greenbin.wms.gui.components.LineChart;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Account;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** FR_08 barcode data, FR_09 capacity sensors, FR_12 points, FR_14 wrong disposals. */
public class SmartBinsScreen extends Screen {
    private final JPanel grid = new JPanel(new GridLayout(0, 5, 10, 10));
    private final List<BinGraphic> graphics = new ArrayList<>();
    private SmartContainer selected;
    private final CodeViews.BarcodeView barcode = new CodeViews.BarcodeView(0);
    private final JTextArea details = new JTextArea(6, 22);
    private final LineChart history = new LineChart();
    private final BarChart trends = new BarChart();
    private final DefaultListModel<String> errors = new DefaultListModel<>();
    private final DefaultListModel<String> maintenance = new DefaultListModel<>();
    private final DefaultTableModel inventory = Tables.model("Bin type", "In stock");
    private final JToggleButton live = new JToggleButton("Live sensors: OFF");
    private final Random random = new Random();
    private int ticks;

    public SmartBinsScreen(Account me) {
        super(me, "Smart Bins", "Every bin reports its fill level. At 90% the manager is alerted and a collection is planned automatically.",
                "FR_08", "FR_09", "FR_12", "FR_14");
        live.setFont(Theme.BOLD);
        live.addActionListener(e -> live.setText(live.isSelected() ? "Live sensors: ON (bins fill up)" : "Live sensors: OFF"));
        body().add(row(FlatButton.primary("Read all sensors now", e -> readSensors()), live,
                FlatButton.secondary("Empty selected bin", e -> emptySelected()),
                FlatButton.danger("Break / repair sensor", e -> toggleSensor()),
                muted("Click a bin to see its details.")), BorderLayout.NORTH);

        grid.setOpaque(false);
        JScrollPane gridScroll = new JScrollPane(grid);
        gridScroll.setBorder(null);
        gridScroll.getViewport().setBackground(Theme.BG);
        gridScroll.getVerticalScrollBar().setUnitIncrement(16);
        body().add(gridScroll, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(330, 100));
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(Theme.NORMAL);
        JPanel det = new JPanel(new BorderLayout(0, 6));
        det.setOpaque(false);
        det.add(barcode, BorderLayout.NORTH);
        det.add(details, BorderLayout.CENTER);
        right.add(Theme.card("Selected bin (barcode FR_08)", det), BorderLayout.NORTH);
        history.setPreferredSize(new Dimension(300, 200));
        right.add(Theme.card("Fill level history (FR_09 log)", history), BorderLayout.CENTER);
        body().add(right, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BOLD);
        tabs.setPreferredSize(new Dimension(100, 230));
        tabs.addTab("Disposal trends (barcode data)", trends);
        tabs.addTab("Wrong disposals (FR_14)", new JScrollPane(new JList<>(errors)));
        tabs.addTab("Maintenance requests", new JScrollPane(new JList<>(maintenance)));
        tabs.addTab("Warehouse inventory (FR_11)", Theme.table(Theme.newTable(inventory)));
        body().add(tabs, BorderLayout.SOUTH);
    }

    private void select(SmartContainer c) {
        selected = c;
        for (BinGraphic g : graphics) g.setSelected(g.getContainer() == c);
        showSelected();
    }

    private void showSelected() {
        if (selected == null) {
            details.setText("No bin selected.");
            return;
        }
        barcode.setSerial(selected.getBarcode().getSerialNumber());
        StringBuilder sb = new StringBuilder();
        sb.append(selected.getKind()).append(" at ").append(selected.getLocation()).append('\n');
        sb.append(String.format("Fill: %.1f / %.0f kg%s%n", selected.getCurrentFillLevel(), selected.getCapacity(),
                selected.getSensor().isWorking() ? "" : "  (SENSOR BROKEN)"));
        sb.append("Barcode scans: ").append(selected.getBarcode().getScanCount()).append('\n');
        if (selected instanceof SmartBin) {
            SmartBin b = (SmartBin) selected;
            sb.append("Wrong disposals this week: ").append(b.countRecentErrors(7)).append('\n');
            sb.append("Bin display: \"").append(b.getDisplayMessage()).append("\"");
        }
        details.setText(sb.toString());
        List<String> x = new ArrayList<>();
        for (int i = 0; i < selected.getFillHistory().size(); i++) x.add(String.valueOf(i + 1));
        history.setData(x, java.util.Collections.singletonList(new LineChart.Series("fill %", Theme.ORANGE,
                new ArrayList<>(selected.getFillHistory()))), -1, "% full");
    }

    private void readSensors() {
        List<String> events = sys.readAllSensors();
        info(events.isEmpty() ? "All sensors read. No new alerts." : "Sensor alerts:\n- " + String.join("\n- ", events));
        refresh();
    }

    private void emptySelected() {
        if (!(selected instanceof SmartBin)) {
            error("Select a smart bin first.");
            return;
        }
        int flagged = 0;
        for (com.greenbin.wms.bins.Waste w : sys.emptyBin((SmartBin) selected)) if (w.isFlagged()) flagged++;
        info("Bin emptied and taken to the recycling plant." + (flagged > 0 ? "\n" + flagged
                + " wrongly thrown items went to the truck's separate compartment (FR_17)." : ""));
        refresh();
    }

    private void toggleSensor() {
        if (selected == null) {
            error("Select a bin first.");
            return;
        }
        selected.getSensor().setWorking(!selected.getSensor().isWorking());
        if (!selected.getSensor().isWorking()) sys.checkContainer(selected);
        refresh();
    }

    @Override
    public void refresh() {
        grid.removeAll();
        graphics.clear();
        for (SmartContainer c : sys.getContainers()) {
            BinGraphic g = new BinGraphic(c, this::select);
            g.setSelected(c == selected);
            graphics.add(g);
            grid.add(g);
        }
        grid.revalidate();
        grid.repaint();
        if (selected == null && !sys.getContainers().isEmpty()) select(sys.getContainers().get(0));
        showSelected();

        Map<WasteType, Double> kg = sys.getDisposedKgByType();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        List<java.awt.Color> colors = new ArrayList<>();
        for (WasteType t : WasteType.values()) {
            labels.add(t.getLabel());
            values.add(kg.get(t));
            colors.add(Theme.colorOf(t));
        }
        trends.setData(labels, values, colors, String.format("kg thrown per type  |  %d scans, %.0f%% in the right bin",
                sys.getDisposals().size(), sys.getCorrectDisposalRate()));

        errors.clear();
        for (SmartBin b : sys.getBins()) for (WasteReport r : b.getErrors()) errors.addElement(r.getTimestamp().toLocalDate() + "  " + r.showReport());
        maintenance.clear();
        for (String m : sys.getMaintenanceRequests()) maintenance.addElement(m);
        inventory.setRowCount(0);
        for (WasteType t : WasteType.values()) {
            int stock = sys.getInventory().getStock(t);
            inventory.addRow(new Object[]{t, stock == 0 ? "OUT OF STOCK" : String.valueOf(stock)});
        }
    }

    @Override
    public void onTick() {
        if (!live.isSelected()) return;
        ticks++;
        if (ticks % 6 == 0) {          // every 3 seconds
            sys.simulateSensorTick(random);
            for (BinGraphic g : graphics) g.repaint();
            if (selected != null) showSelected();
        }
    }
}
