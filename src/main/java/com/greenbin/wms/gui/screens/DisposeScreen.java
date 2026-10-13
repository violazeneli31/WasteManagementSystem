package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.DisposalRecord;
import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BinGraphic;
import com.greenbin.wms.gui.components.CodeViews;
import com.greenbin.wms.gui.components.MapView;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/** FR_08 scan the bin's barcode, FR_12 earn points, FR_14 get told when the bin is wrong. */
public class DisposeScreen extends Screen {
    private final User user;
    private final MapView map;
    private final JComboBox<SmartBin> bin = new JComboBox<>();
    private final JComboBox<WasteType> what = new JComboBox<>(WasteType.values());
    private final JSlider weight = new JSlider(1, 20, 3);
    private final JLabel weightLabel = Theme.label("3 kg", Theme.BOLD, Theme.TEXT);
    private final CodeViews.BarcodeView barcode = new CodeViews.BarcodeView(0);
    private final JPanel binView = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private final JLabel result = Theme.label(" ", Theme.H1, Theme.TEXT);
    private final JLabel resultDetail = Theme.label(" ", Theme.NORMAL, Theme.TEXT);
    private final JLabel points = Theme.label("", Theme.H2, Theme.GREEN);

    public DisposeScreen(Account me) {
        super(me, "Throw Waste", "Pick a bin on the map (or the list), scan its barcode and throw your waste", "FR_08", "FR_12", "FR_14");
        user = (User) me;
        map = new MapView(sys);
        map.setShowVehicles(false);
        map.onSelect(o -> { if (o instanceof SmartBin) bin.setSelectedItem(o); });
        body().add(Theme.card("Bins near you - click one", map), BorderLayout.CENTER);

        bin.addActionListener(e -> showBin());
        weight.addChangeListener(e -> weightLabel.setText(weight.getValue() + " kg"));
        weight.setOpaque(false);

        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(380, 100));
        JPanel form = new Stack();
        form.setOpaque(false);
        form.add(muted("1. The bin"));
        form.add(bin);
        form.add(barcode);
        form.add(muted("2. What are you throwing?"));
        form.add(what);
        form.add(row(muted("Weight"), weight, weightLabel));
        form.add(FlatButton.primary("3. Scan barcode & throw", e -> dispose()));
        side.add(Theme.card("Scan & throw", form), BorderLayout.NORTH);

        JPanel res = new JPanel(new BorderLayout(0, 4));
        res.setOpaque(false);
        binView.setOpaque(false);
        res.add(binView, BorderLayout.WEST);
        JPanel texts = new JPanel(new GridLayout(0, 1));
        texts.setOpaque(false);
        texts.add(result);
        texts.add(resultDetail);
        texts.add(points);
        res.add(texts, BorderLayout.CENTER);
        side.add(Theme.card("Result", res), BorderLayout.CENTER);
        body().add(side, BorderLayout.EAST);
    }

    private void showBin() {
        SmartBin b = (SmartBin) bin.getSelectedItem();
        if (b == null) return;
        barcode.setSerial(b.getBarcode().getSerialNumber());
        map.setSelected(b);
        binView.removeAll();
        binView.add(new BinGraphic(b, c -> { }));
        binView.revalidate();
        binView.repaint();
    }

    private void dispose() {
        SmartBin b = (SmartBin) bin.getSelectedItem();
        if (b == null) return;
        result.setText("Scanning...");
        result.setForeground(Theme.MUTED);
        Timer t = new Timer(600, e -> {
            try {
                DisposalRecord r = sys.disposeWaste(user, b, (WasteType) what.getSelectedItem(), weight.getValue());
                if (r.isCorrect()) {
                    result.setText("Right bin!");
                    result.setForeground(Theme.GREEN);
                    resultDetail.setText("<html>Thank you for recycling.<br>Bin display: \"" + b.getDisplayMessage() + "\"</html>");
                } else {
                    result.setText("Wrong bin!");
                    result.setForeground(Theme.RED);
                    resultDetail.setText("<html>" + r.getWaste().getType() + " goes in a <b>" + r.getWaste().getType()
                            + "</b> bin.<br>Bin display: \"" + b.getDisplayMessage() + "\"<br>Only part of the points were given.</html>");
                }
                points.setText("+" + r.getPoints() + " points  (total " + user.getPoints() + ")");
                points.setForeground(r.getPoints() > 0 ? Theme.GREEN : new Color(0x9E9E9E));
            } catch (IllegalStateException ex) {
                error(ex.getMessage());
            }
            showBin();
        });
        t.setRepeats(false);
        t.start();
    }

    @Override
    public void refresh() {
        Object sel = bin.getSelectedItem();
        bin.removeAllItems();
        for (SmartBin b : sys.getBins()) bin.addItem(b);
        if (sel != null) bin.setSelectedItem(sel);
        showBin();
    }
}
