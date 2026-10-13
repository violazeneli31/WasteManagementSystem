package com.greenbin.wms.gui.components;

import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.bins.SmartContainer;
import com.greenbin.wms.gui.Theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** Draws one smart container as a bin with its fill level (FR_09). Click to select it. */
public class BinGraphic extends JComponent {
    private final SmartContainer container;
    private boolean selected;

    public BinGraphic(SmartContainer container, Consumer<SmartContainer> onClick) {
        this.container = container;
        setPreferredSize(new Dimension(128, 168));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(container.getKind() + " - barcode " + container.getBarcode());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { onClick.accept(container); }
        });
    }

    public SmartContainer getContainer() { return container; }
    public void setSelected(boolean selected) { this.selected = selected; repaint(); }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.smooth(g);
        int w = getWidth();
        g.setColor(selected ? new Color(0xFFF8E1) : Color.WHITE);
        g.fillRoundRect(0, 0, w - 1, getHeight() - 1, 14, 14);
        g.setColor(selected ? Theme.YELLOW : Theme.BORDER);
        g.setStroke(new BasicStroke(selected ? 2.5f : 1f));
        g.drawRoundRect(0, 0, w - 1, getHeight() - 1, 14, 14);

        Color body = container instanceof SmartBin ? Theme.colorOf(((SmartBin) container).getWasteType()) : new Color(0xAD1457);
        int bx = w / 2 - 30, by = 26, bw = 60, bh = 78;
        // lid
        g.setColor(body.darker());
        g.fillRoundRect(bx - 5, by - 10, bw + 10, 10, 6, 6);
        g.fillRect(bx + bw / 2 - 8, by - 15, 16, 6);
        // body (a trapezoid)
        Polygon shape = new Polygon(new int[]{bx, bx + bw, bx + bw - 6, bx + 6}, new int[]{by, by, by + bh, by + bh}, 4);
        g.setColor(new Color(body.getRed(), body.getGreen(), body.getBlue(), 60));
        g.fillPolygon(shape);
        // fill level
        boolean working = container.getSensor().isWorking();
        double pct = container.getFillPercent();
        if (working) {
            int fillH = (int) Math.round(bh * pct / 100);
            g.setClip(shape);
            g.setColor(Theme.fillColor(pct));
            g.fillRect(bx, by + bh - fillH, bw, fillH);
            g.setClip(null);
        }
        g.setColor(body);
        g.setStroke(new BasicStroke(2.5f));
        g.drawPolygon(shape);

        g.setFont(Theme.H2);
        String txt = working ? Math.round(pct) + "%" : "?";
        g.setColor(working && pct > 45 ? Color.WHITE : Theme.TEXT);
        g.drawString(txt, w / 2 - g.getFontMetrics().stringWidth(txt) / 2, by + bh / 2 + 14);

        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        g.setColor(Theme.TEXT);
        String kind = container instanceof SmartBin ? ((SmartBin) container).getWasteType().getLabel() : "Donations";
        center(g, kind, by + bh + 18);
        g.setFont(Theme.SMALL);
        g.setColor(Theme.MUTED);
        String loc = container.getLocation();
        if (loc.length() > 19) loc = loc.substring(0, 18) + ".";
        center(g, loc, by + bh + 32);
        String status;
        Color statusColor;
        if (!working) { status = "SENSOR BROKEN"; statusColor = Theme.RED; }
        else if (container instanceof SmartBin) {
            status = ((SmartBin) container).getStatus().toString();
            statusColor = Theme.fillColor(pct);
        } else { status = "DONATIONS"; statusColor = new Color(0xAD1457); }
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        g.setColor(statusColor);
        center(g, status, by + bh + 46);
        g.dispose();
    }

    private void center(Graphics2D g, String s, int y) {
        g.drawString(s, getWidth() / 2 - g.getFontMetrics().stringWidth(s) / 2, y);
    }
}
