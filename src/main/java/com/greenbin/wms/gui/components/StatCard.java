package com.greenbin.wms.gui.components;

import com.greenbin.wms.gui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/** A small card with a big number, e.g. "3 bins full". */
public class StatCard extends JPanel {
    private final JLabel value = Theme.label("-", Theme.BIG_NUMBER, Theme.TEXT);
    private final JLabel caption;
    private final Color accent;

    public StatCard(String caption, Color accent) {
        super(new BorderLayout());
        this.accent = accent;
        this.caption = Theme.label(caption, Theme.SMALL, Theme.MUTED);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 12)));
        value.setForeground(accent.darker());
        add(value, BorderLayout.CENTER);
        add(this.caption, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(170, 78));
    }

    public void setValue(String text) { value.setText(text); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(accent);
        g.fillRect(0, 0, 5, getHeight());
    }
}
