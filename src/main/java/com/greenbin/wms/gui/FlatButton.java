package com.greenbin.wms.gui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;

/** A simple coloured button that looks the same on Windows, macOS and Linux. */
public class FlatButton extends JButton {
    private Color color;

    public FlatButton(String text, Color color) {
        super(text);
        this.color = color;
        setFont(Theme.BOLD);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);
    }

    public FlatButton(String text, Color color, ActionListener action) {
        this(text, color);
        addActionListener(action);
    }

    /** A green main button. */
    public static FlatButton primary(String text, ActionListener action) { return new FlatButton(text, Theme.GREEN, action); }

    /** A grey secondary button. */
    public static FlatButton secondary(String text, ActionListener action) { return new FlatButton(text, new Color(0x607D8B), action); }

    /** A red button for "dangerous" actions. */
    public static FlatButton danger(String text, ActionListener action) { return new FlatButton(text, Theme.RED, action); }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width + 16, Math.max(32, d.height + 6));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.smooth(g2);
        Color c = !isEnabled() ? new Color(0xB0BEC5) : getModel().isPressed() ? color.darker()
                : getModel().isRollover() ? color.brighter() : color;
        g2.setColor(c);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }
}
