package com.greenbin.wms.gui;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** A small rounded label, e.g. "FR_01" or a status. */
public class Chip extends JLabel {
    private Color color;

    public Chip(String text, Color color) {
        super(text);
        this.color = color;
        setFont(Theme.SMALL.deriveFont(java.awt.Font.BOLD));
        setForeground(color.darker());
        setHorizontalAlignment(CENTER);
    }

    public void setChip(String text, Color color) {
        setText(text);
        this.color = color;
        setForeground(color.darker());
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width + 16, d.height + 6);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 35));
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
