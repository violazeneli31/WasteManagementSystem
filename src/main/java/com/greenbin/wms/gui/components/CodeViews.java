package com.greenbin.wms.gui.components;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Random;

/** Drawings of a barcode (FR_08) and a QR code (FR_25) made from a number / text. */
public final class CodeViews {
    private CodeViews() { }

    /** A barcode made from the container's serial number. */
    public static class BarcodeView extends JComponent {
        private long serial;

        public BarcodeView(long serial) {
            this.serial = serial;
            setPreferredSize(new Dimension(200, 70));
        }

        public void setSerial(long serial) { this.serial = serial; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            String digits = String.valueOf(serial);
            int x = 10;
            for (char ch : digits.toCharArray()) {
                int d = ch - '0';
                for (int b = 0; b < 4 && x < getWidth() - 10; b++) {
                    int width = ((d >> b) & 1) == 1 ? 3 : 1;
                    g.fillRect(x, 6, width, getHeight() - 26);
                    x += width + 2;
                }
            }
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.drawString(digits, getWidth() / 2 - g.getFontMetrics().stringWidth(digits) / 2, getHeight() - 6);
            g.dispose();
        }
    }

    /** A QR-style code made from the voucher code (the same code always gives the same picture). */
    public static class QrCodeView extends JComponent {
        private static final int N = 25;
        private String code;

        public QrCodeView(String code) {
            this.code = code;
            setPreferredSize(new Dimension(150, 150));
        }

        public void setCode(String code) { this.code = code; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int size = Math.min(getWidth(), getHeight());
            int cell = size / (N + 2);
            int ox = (getWidth() - cell * N) / 2, oy = (getHeight() - cell * N) / 2;
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (code == null) { g.dispose(); return; }
            Random r = new Random(code.hashCode());
            g.setColor(Color.BLACK);
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    if (inFinder(x, y)) continue;
                    if (r.nextBoolean()) g.fillRect(ox + x * cell, oy + y * cell, cell, cell);
                }
            }
            finder(g, ox, oy, cell);
            finder(g, ox + (N - 7) * cell, oy, cell);
            finder(g, ox, oy + (N - 7) * cell, cell);
            g.dispose();
        }

        private boolean inFinder(int x, int y) {
            return (x < 8 && y < 8) || (x >= N - 8 && y < 8) || (x < 8 && y >= N - 8);
        }

        private void finder(Graphics2D g, int x, int y, int cell) {
            g.setColor(Color.BLACK);
            g.fillRect(x, y, 7 * cell, 7 * cell);
            g.setColor(Color.WHITE);
            g.fillRect(x + cell, y + cell, 5 * cell, 5 * cell);
            g.setColor(Color.BLACK);
            g.fillRect(x + 2 * cell, y + 2 * cell, 3 * cell, 3 * cell);
        }
    }
}
