package com.greenbin.wms.gui;

import com.greenbin.wms.people.Account;
import com.greenbin.wms.system.GreenBinSystem;

import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.Dimension;

/** The application window. It shows the login screen or the dashboard, and runs the live timer. */
public class AppFrame extends JFrame {
    private Dashboard dashboard;

    public AppFrame() {
        super("GreenBin Albania - Waste Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1380, 880);
        setMinimumSize(new Dimension(1150, 740));
        setLocationRelativeTo(null);
        showLogin();

        // the "real time" part: trucks move, screens update (FR_06)
        Timer timer = new Timer(500, e -> {
            GreenBinSystem.getInstance().tick();
            if (dashboard != null) dashboard.onTick();
        });
        timer.start();
    }

    public void showLogin() {
        dashboard = null;
        setContentPane(new LoginPanel(this::openDashboard));
        revalidate();
        repaint();
    }

    public void openDashboard(Account account) {
        dashboard = new Dashboard(this, account);
        setContentPane(dashboard);
        revalidate();
        repaint();
    }

    public void resetDemo() {
        GreenBinSystem.resetDemo();
        showLogin();
    }
}
