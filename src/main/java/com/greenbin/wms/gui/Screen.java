package com.greenbin.wms.gui;

import com.greenbin.wms.people.Account;
import com.greenbin.wms.system.GreenBinSystem;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Base class of every screen: a title, the FR numbers it covers, and the content.
 * refresh() is called every time the screen is opened, so it always shows the latest data.
 */
public abstract class Screen extends JPanel {
    protected final GreenBinSystem sys;
    protected final Account me;
    private final JPanel body = new JPanel(new BorderLayout(12, 12));

    protected Screen(Account me, String title, String subtitle, String... frCodes) {
        super(new BorderLayout());
        this.sys = GreenBinSystem.getInstance();
        this.me = me;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(Theme.label(title, Theme.H1, Theme.TEXT), BorderLayout.NORTH);
        titles.add(Theme.label(subtitle, Theme.NORMAL, Theme.MUTED), BorderLayout.SOUTH);
        header.add(titles, BorderLayout.CENTER);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        chips.setOpaque(false);
        for (String fr : frCodes) chips.add(new Chip(fr, Theme.GREEN));
        header.add(chips, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        body.setOpaque(false);
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    /** Where subclasses put their content. */
    protected JPanel body() { return body; }

    /** Reloads the data shown on the screen. */
    public abstract void refresh();

    /** Called about twice a second by the live timer (only for the visible screen). */
    public void onTick() { }

    protected void info(String message) {
        JOptionPane.showMessageDialog(this, message, "GreenBin", JOptionPane.INFORMATION_MESSAGE);
    }

    /** NFR_03: clear error messages. */
    protected void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Something is not right", JOptionPane.WARNING_MESSAGE);
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    protected static JPanel row(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        for (JComponent c : items) p.add(c);
        return p;
    }

    protected static JLabel muted(String text) { return Theme.label(text, Theme.SMALL, Theme.MUTED); }
}
