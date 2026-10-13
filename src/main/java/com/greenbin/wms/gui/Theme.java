package com.greenbin.wms.gui;

import com.greenbin.wms.model.WasteType;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.Graphics2D;

/** Colours, fonts and small helpers so every screen looks the same (NFR_02 consistent layout). */
public final class Theme {
    private Theme() { }

    public static final Color GREEN_DARK = new Color(0x1B5E20);
    public static final Color GREEN = new Color(0x2E7D32);
    public static final Color GREEN_LIGHT = new Color(0xE8F5E9);
    public static final Color BG = new Color(0xF2F5F3);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xDCE4DE);
    public static final Color TEXT = new Color(0x1F2A24);
    public static final Color MUTED = new Color(0x6B7A71);
    public static final Color RED = new Color(0xD32F2F);
    public static final Color ORANGE = new Color(0xEF6C00);
    public static final Color BLUE = new Color(0x1565C0);
    public static final Color YELLOW = new Color(0xF9A825);
    public static final Color PURPLE = new Color(0x6A1B9A);

    public static final String FONT = "SansSerif";
    public static final Font NORMAL = new Font(FONT, Font.PLAIN, 13);
    public static final Font SMALL = new Font(FONT, Font.PLAIN, 11);
    public static final Font BOLD = new Font(FONT, Font.BOLD, 13);
    public static final Font H2 = new Font(FONT, Font.BOLD, 15);
    public static final Font H1 = new Font(FONT, Font.BOLD, 22);
    public static final Font BIG_NUMBER = new Font(FONT, Font.BOLD, 26);
    public static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);

    public static Color colorOf(WasteType t) {
        if (t == null) return MUTED;
        switch (t) {
            case PAPER: return new Color(0x1E88E5);
            case PLASTIC: return new Color(0xF9A825);
            case METAL_AND_GLASS: return new Color(0x00897B);
            case ORGANIC_WASTE: return new Color(0x8D6E63);
            default: return new Color(0xE53935);
        }
    }

    /** Green under 70%, orange under 90%, red when full. */
    public static Color fillColor(double percent) {
        if (percent >= 90) return RED;
        if (percent >= 70) return ORANGE;
        return new Color(0x43A047);
    }

    /** Nimbus look and feel with green accents. */
    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) UIManager.setLookAndFeel(info.getClassName());
            }
        } catch (Exception ignored) {
            // the default look and feel is fine too
        }
        UIManager.put("control", BG);
        UIManager.put("nimbusBase", new Color(0x4A6B55));
        UIManager.put("nimbusFocus", new Color(0x66BB6A));
        UIManager.put("nimbusSelectionBackground", new Color(0x43A047));
        UIManager.put("nimbusSelection", new Color(0x43A047));
        UIManager.put("Table.alternateRowColor", new Color(0xF6F9F7));
        UIManager.put("defaultFont", NORMAL);
        UIManager.put("ToolTip.font", NORMAL);
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static Border padding(int p) { return BorderFactory.createEmptyBorder(p, p, p, p); }

    /** A white card with a title. */
    public static JPanel card(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), padding(12)));
        if (title != null) card.add(label(title, H2, TEXT), BorderLayout.NORTH);
        if (content != null) card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** A table that looks the same everywhere, inside a scroll pane. */
    public static JScrollPane table(JTable table) {
        table.setRowHeight(26);
        table.setFont(NORMAL);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        JTableHeader header = table.getTableHeader();
        header.setFont(BOLD);
        header.setReorderingAllowed(false);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static JTable newTable(TableModel model) {
        JTable t = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        return t;
    }

    /** Colours a table cell by its text (e.g. "FULL" red, "PAID" green). */
    public static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            String v = String.valueOf(value).toUpperCase();
            if (!selected) {
                Color color = TEXT;
                if (v.contains("FULL") || v.contains("OVERDUE") || v.contains("SUSPICIOUS") || v.contains("FRAUD")
                        || v.contains("NEEDS") || v.contains("DENIED") || v.contains("ISSUE") || v.contains("REJECTED")
                        || v.contains("BROKEN") || v.contains("OPEN") || v.contains("BLOCKED")) color = RED;
                else if (v.contains("WARNING") || v.contains("UNPAID") || v.contains("REQUESTED") || v.contains("MOVED")
                        || v.contains("ORDERED") || v.contains("REVIEW") || v.contains("ASSIGNED") || v.contains("PLANNED")
                        || v.contains("WAITING")) color = ORANGE;
                else if (v.contains("PAID") || v.contains("CONFIRMED") || v.contains("ACTIVE") || v.contains("OK")
                        || v.contains("PLACED") || v.contains("RESOLVED") || v.contains("APPROVED") || v.contains("LEGITIMATE")
                        || v.contains("DONE") || v.contains("REDEEMED")) color = GREEN;
                c.setForeground(color);
            }
            c.setFont(BOLD);
            return c;
        }
    }
}
