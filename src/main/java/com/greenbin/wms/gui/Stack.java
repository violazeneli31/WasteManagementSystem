package com.greenbin.wms.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

/** A simple vertical form: every field keeps its natural height and uses the full width. */
public class Stack extends JPanel {
    public Stack() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }

    @Override
    public Component add(Component c) {
        if (c instanceof JComponent) {
            JComponent j = (JComponent) c;
            j.setAlignmentX(Component.LEFT_ALIGNMENT);
            j.setMaximumSize(new Dimension(Integer.MAX_VALUE, j.getPreferredSize().height));
        }
        if (getComponentCount() > 0) super.add(Box.createVerticalStrut(5));
        return super.add(c);
    }
}
