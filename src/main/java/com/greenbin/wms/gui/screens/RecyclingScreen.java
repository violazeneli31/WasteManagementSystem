package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.gui.components.StatCard;
import com.greenbin.wms.model.Role;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.RecyclingFacilityOperator;
import com.greenbin.wms.recycling.RecyclingFacility;
import com.greenbin.wms.recycling.RecyclingFacility.Batch;
import com.greenbin.wms.recycling.RecyclingFacility.Stage;
import com.greenbin.wms.recycling.RecyclingReport;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** FR_17 flagged waste check, FR_18 sorting by bin + metal/glass separation, FR_19 stage and conversion tracking. */
public class RecyclingScreen extends Screen {
    private final boolean operator;
    private final RecyclingFacility plant;
    private final StatCard[] stageCards = new StatCard[4];
    private final StatCard efficiency = new StatCard("conversion efficiency", Theme.PURPLE);
    private final StatCard revenue = new StatCard("revenue from material (Lek)", Theme.GREEN);
    private final Pipeline pipeline = new Pipeline();
    private final DefaultListModel<Waste> flagged = new DefaultListModel<>();
    private final DefaultListModel<Waste> inspection = new DefaultListModel<>();
    private final JList<Waste> inspectionList = new JList<>(inspection);
    private final JComboBox<WasteType> correctType = new JComboBox<>(WasteType.values());
    private final BarChart conversion = new BarChart();
    private final JTextArea report = new JTextArea(10, 30);
    private RecyclingReport lastReport;
    private final List<JComponent> operatorOnly = new ArrayList<>();

    public RecyclingScreen(Account me) {
        super(me, "Recycling Plant", "Waste arrives from the trucks, is checked, sorted by bin type, processed and recycled",
                "FR_17", "FR_18", "FR_19");
        operator = me.getRole() == Role.RECYCLING_OPERATOR;
        plant = sys.getFacility();

        JPanel cards = new JPanel(new GridLayout(1, 6, 8, 0));
        cards.setOpaque(false);
        Color[] colors = {new Color(0x78909C), Theme.BLUE, Theme.ORANGE, Theme.GREEN};
        for (int i = 0; i < 4; i++) {
            stageCards[i] = new StatCard("kg " + Stage.values()[i].name().toLowerCase(), colors[i]);
            cards.add(stageCards[i]);
        }
        cards.add(efficiency);
        cards.add(revenue);
        body().add(cards, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(pipeline, BorderLayout.CENTER);
        JPanel steps = new JPanel(new GridLayout(1, 3, 8, 0));
        steps.setOpaque(false);
        center.add(steps, BorderLayout.SOUTH);
        addAll(steps, op(FlatButton.primary("1. Sort received waste", e -> { plant.sortReceivedWaste(); refresh(); })),
                op(FlatButton.primary("2. Separate metal & glass", e -> {
                    double kg = plant.separateMetalFromGlass();
                    info(kg == 0 ? "No sorted 'Metal & Glass' to separate." : String.format("%.0f kg separated into Metal and Glass.", kg));
                    refresh();
                })),
                op(FlatButton.primary("3. Move clicked batch to next stage", e -> moveSelected())));
        body().add(Theme.card("Processing stages (FR_19)", center), BorderLayout.CENTER);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 8));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(320, 100));
        JPanel f1 = new JPanel(new BorderLayout(0, 6));
        f1.setOpaque(false);
        f1.add(new JScrollPane(new JList<>(flagged)), BorderLayout.CENTER);
        f1.add(op(FlatButton.primary("Check with AI camera", e -> {
            int confirmed = plant.verifyFlaggedWaste(sys.getRecognition());
            info(confirmed + " item(s) confirmed as wrong -> manual inspection.\nThe others were false alarms and went to sorting.");
            refresh();
        })), BorderLayout.SOUTH);
        left.add(Theme.card("Truck's separate compartment (FR_17)", f1));
        JPanel f2 = new JPanel(new BorderLayout(0, 6));
        f2.setOpaque(false);
        f2.add(new JScrollPane(inspectionList), BorderLayout.CENTER);
        f2.add(row(muted("Real type:"), correctType, op(FlatButton.secondary("Correct", e -> correct()))), BorderLayout.SOUTH);
        left.add(Theme.card("Manual inspection", f2));
        body().add(left, BorderLayout.WEST);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(100, 250));
        bottom.add(Theme.card("Recycled: kg in vs kg of new material out", conversion));
        JPanel rep = new JPanel(new BorderLayout(0, 6));
        rep.setOpaque(false);
        report.setEditable(false);
        report.setFont(Theme.MONO);
        rep.add(new JScrollPane(report), BorderLayout.CENTER);
        rep.add(row(FlatButton.primary("Generate report", e -> generateReport()),
                op(FlatButton.secondary("Confirm report", e -> {
                    if (lastReport == null) { error("Generate a report first."); return; }
                    lastReport.confirm();
                    report.setText(lastReport.showReport());
                    sys.getLog().add("RECYCLING", "Report " + lastReport.getReportID() + " confirmed by " + me.getDisplayName());
                })),
                op(FlatButton.danger("Report a problem", e -> {
                    String p = javax.swing.JOptionPane.showInputDialog(this, "Describe the problem (e.g. tracking error, machine stopped):");
                    if (p != null && !p.isBlank()) {
                        plant.reportIssue(p.trim());
                        sys.submitReport(me, "Recycling plant problem", p.trim());
                        info("The problem was logged and sent to the manager.");
                    }
                }))), BorderLayout.SOUTH);
        bottom.add(Theme.card("Recycling report", rep));
        body().add(bottom, BorderLayout.SOUTH);
    }

    private static void addAll(JPanel p, JComponent... cs) {
        for (JComponent c : cs) p.add(c);
    }

    private JComponent op(JComponent c) {
        operatorOnly.add(c);
        c.setEnabled(operator);
        if (!operator) c.setToolTipText("Only the recycling operator can do this");
        return c;
    }

    private void moveSelected() {
        Batch b = pipeline.selected;
        if (b == null) { error("Click a batch block first."); return; }
        if (!plant.moveToNextStage(b)) {
            error(b.getStage() == Stage.RECYCLED ? "This batch is already recycled."
                    : b.getStage() == Stage.RECEIVED ? "Sort the received waste first (button 1)."
                    : "Metal & Glass must be separated first (button 2).");
            return;
        }
        refresh();
    }

    private void correct() {
        Waste w = inspectionList.getSelectedValue();
        if (w == null) { error("Select an item in manual inspection."); return; }
        plant.correctMisclassifiedWaste(w, (WasteType) correctType.getSelectedItem(), (RecyclingFacilityOperator) me);
        refresh();
    }

    private void generateReport() {
        RecyclingFacilityOperator op = operator ? (RecyclingFacilityOperator) me : sys.getOperator();
        lastReport = plant.generateReport(op);
        report.setText(lastReport.showReport());
        report.setCaretPosition(0);
    }

    @Override
    public void refresh() {
        for (int i = 0; i < 4; i++) stageCards[i].setValue(String.format("%,.0f", plant.getKgAtStage(Stage.values()[i])));
        efficiency.setValue(String.format("%.1f%%", plant.getEfficiency()));
        revenue.setValue(String.format("%,.0f", plant.getRevenue()));
        flagged.clear();
        for (Waste w : plant.getFlaggedCompartment()) flagged.addElement(w);
        inspection.clear();
        for (Waste w : plant.getManualInspection()) inspection.addElement(w);

        List<String> labels = new ArrayList<>();
        List<Double> in = new ArrayList<>();
        List<Double> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : plant.getConversionData().entrySet()) {
            labels.add(e.getKey());
            in.add(e.getValue()[0]);
            out.add(e.getValue()[1]);
        }
        conversion.setData(labels, in, java.util.Collections.singletonList(Theme.GREEN), "kg");
        conversion.setSecondSeries(out, "kg in", "kg out");
        pipeline.repaint();
    }

    /** Draws the four stages as columns with one coloured block per batch. */
    private class Pipeline extends JComponent {
        Batch selected;
        private final Map<Rectangle, Batch> hitboxes = new HashMap<>();

        Pipeline() {
            setPreferredSize(new Dimension(600, 260));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (Map.Entry<Rectangle, Batch> h : hitboxes.entrySet()) if (h.getKey().contains(e.getPoint())) selected = h.getValue();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            hitboxes.clear();
            int w = getWidth(), h = getHeight();
            int colW = (w - 30) / 4;
            String[] names = {"RECEIVED", "SORTED", "PROCESSING", "RECYCLED"};
            for (int s = 0; s < 4; s++) {
                int x = s * (colW + 10);
                g.setColor(new Color(0xF5F7F6));
                g.fillRoundRect(x, 0, colW, h - 1, 12, 12);
                g.setColor(Theme.BORDER);
                g.drawRoundRect(x, 0, colW, h - 1, 12, 12);
                g.setFont(Theme.BOLD);
                g.setColor(Theme.TEXT);
                g.drawString(names[s], x + 10, 20);
                if (s < 3) {
                    g.setColor(Theme.MUTED);
                    g.fillPolygon(new int[]{x + colW + 1, x + colW + 9, x + colW + 1}, new int[]{h / 2 - 6, h / 2, h / 2 + 6}, 3);
                }
                int y = 30;
                for (Batch b : plant.getBatches(Stage.values()[s])) {
                    int bh = (int) Math.max(26, Math.min(70, b.getKg() / 25));
                    Rectangle r = new Rectangle(x + 8, y, colW - 16, bh);
                    hitboxes.put(r, b);
                    Color c = materialColor(b.getMaterial());
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
                    g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                    g.setColor(c);
                    g.setStroke(new BasicStroke(b == selected ? 3f : 1.5f));
                    g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                    g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
                    g.setColor(Theme.TEXT);
                    String text = b.getMaterial() + String.format("  %,.0f kg", b.getKg());
                    if (b.getStage() == Stage.RECYCLED) text += String.format(" -> %,.0f kg", b.getOutputKg());
                    g.drawString(text, r.x + 8, r.y + 17);
                    y += bh + 6;
                    if (y > h - 30) break;
                }
            }
            g.dispose();
        }
    }

    static Color materialColor(String m) {
        switch (m) {
            case "Paper": return Theme.colorOf(WasteType.PAPER);
            case "Plastic": return Theme.colorOf(WasteType.PLASTIC);
            case "Organic": return Theme.colorOf(WasteType.ORGANIC_WASTE);
            case "Hazardous": return Theme.colorOf(WasteType.HAZARDOUS);
            case "Metal": return new Color(0x546E7A);
            case "Glass": return new Color(0x26A69A);
            default: return Theme.colorOf(WasteType.METAL_AND_GLASS);
        }
    }
}
