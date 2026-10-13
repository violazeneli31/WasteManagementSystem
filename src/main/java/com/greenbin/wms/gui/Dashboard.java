package com.greenbin.wms.gui;

import com.greenbin.wms.gui.screens.ActivityLogScreen;
import com.greenbin.wms.gui.screens.BillingScreen;
import com.greenbin.wms.gui.screens.BinsWorkScreen;
import com.greenbin.wms.gui.screens.BudgetScreen;
import com.greenbin.wms.gui.screens.ClassifyScreen;
import com.greenbin.wms.gui.screens.CollectorTasksScreen;
import com.greenbin.wms.gui.screens.CommunityScreen;
import com.greenbin.wms.gui.screens.DisposeScreen;
import com.greenbin.wms.gui.screens.DisputesScreen;
import com.greenbin.wms.gui.screens.DonateScreen;
import com.greenbin.wms.gui.screens.ExpensesScreen;
import com.greenbin.wms.gui.screens.FraudScreen;
import com.greenbin.wms.gui.screens.InboxScreen;
import com.greenbin.wms.gui.screens.ManagerHomeScreen;
import com.greenbin.wms.gui.screens.MyBillsScreen;
import com.greenbin.wms.gui.screens.PatternsScreen;
import com.greenbin.wms.gui.screens.PickupRequestsScreen;
import com.greenbin.wms.gui.screens.ProfileScreen;
import com.greenbin.wms.gui.screens.ProgramsScreen;
import com.greenbin.wms.gui.screens.RecyclingScreen;
import com.greenbin.wms.gui.screens.ReportsScreen;
import com.greenbin.wms.gui.screens.RequestPickupScreen;
import com.greenbin.wms.gui.screens.RewardsScreen;
import com.greenbin.wms.gui.screens.RouteScreen;
import com.greenbin.wms.gui.screens.SchedulesScreen;
import com.greenbin.wms.gui.screens.SmartBinsScreen;
import com.greenbin.wms.gui.screens.StatisticsScreen;
import com.greenbin.wms.gui.screens.ZoneMapScreen;
import com.greenbin.wms.people.Account;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The main window after login: header, menu on the left, screen on the right.
 * The menu depends on the role (NFR_13 role-based access control).
 */
public class Dashboard extends JPanel {
    private final AppFrame frame;
    private final Account me;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel menu = new JPanel();
    private final List<NavItem> items = new ArrayList<>();
    private final FlatButton bell;
    private Screen current;
    private InboxScreen inbox;
    private int seenNotifications;

    public Dashboard(AppFrame frame, Account me) {
        super(new BorderLayout());
        this.frame = frame;
        this.me = me;
        bell = new FlatButton("Notifications", new Color(0x388E3C), e -> open(inbox));

        add(buildHeader(), BorderLayout.NORTH);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        buildMenu();
        JScrollPane menuScroll = new JScrollPane(menu);
        menuScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));
        menuScroll.setPreferredSize(new Dimension(236, 100));
        menuScroll.getVerticalScrollBar().setUnitIncrement(16);
        menuScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(menuScroll, BorderLayout.WEST);
        content.setBackground(Theme.BG);
        add(content, BorderLayout.CENTER);

        open(items.get(0).screen);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.smooth(g2);
                LoginPanel.Logo.paint(g2, 14, 9, 38);
                g2.dispose();
            }
        };
        header.setBackground(Theme.GREEN_DARK);
        header.setPreferredSize(new Dimension(100, 56));
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.setBorder(BorderFactory.createEmptyBorder(8, 64, 6, 0));
        title.add(Theme.label("GreenBin Albania", Theme.H2.deriveFont(17f), Color.WHITE), BorderLayout.NORTH);
        title.add(Theme.label("Waste Management System", Theme.SMALL, new Color(0xA5D6A7)), BorderLayout.SOUTH);
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        right.setOpaque(false);
        JLabel who = Theme.label(me.getDisplayName() + "  |  " + me.getRole().getLabel(), Theme.BOLD, Color.WHITE);
        right.add(who);
        right.add(bell);
        right.add(new FlatButton("Reset demo", new Color(0x546E7A), e -> {
            if (javax.swing.JOptionPane.showConfirmDialog(this, "Start again from the demo data?", "Reset",
                    javax.swing.JOptionPane.YES_NO_OPTION) == 0) frame.resetDemo();
        }));
        right.add(new FlatButton("Log out", new Color(0xC62828), e -> frame.showLogin()));
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** The menu of each role. */
    private void buildMenu() {
        inbox = new InboxScreen(me);
        switch (me.getRole()) {
            case MANAGER:
                add("Overview", "dashboard", new ManagerHomeScreen(me));
                section("Collection & transport");
                add("Zone Map & Trucks", "FR_01  FR_06  FR_11", new ZoneMapScreen(me));
                add("Statistics & Forecast", "FR_02  FR_30", new StatisticsScreen(me));
                add("Schedules & Weather", "FR_02  FR_03  FR_05  FR_07", new SchedulesScreen(me));
                add("Route Optimization", "FR_04  FR_06", new RouteScreen(me));
                section("Smart bins & community");
                add("Smart Bins", "FR_08  FR_09  FR_12  FR_14", new SmartBinsScreen(me));
                add("Programs & Events", "FR_15  FR_16", new ProgramsScreen(me));
                add("Reports & Feedback", "FR_20", new ReportsScreen(me));
                add("Community & Partners", "FR_24  FR_25  FR_26  FR_27", new CommunityScreen(me));
                section("Recycling & finance");
                add("Recycling Plant", "FR_17  FR_18  FR_19", new RecyclingScreen(me));
                add("Expenses & Tax", "FR_32  FR_33", new ExpensesScreen(me));
                add("Budget Forecast", "FR_35", new BudgetScreen(me));
                add("Disputes", "FR_34", new DisputesScreen(me));
                section("System");
                add("Notifications", "Observer + Decorator", inbox);
                add("Activity Log", "all decisions", new ActivityLogScreen(me));
                break;
            case GARBAGE_COLLECTOR:
                add("My Tasks", "FR_05  FR_06", new CollectorTasksScreen(me));
                add("Pickup Requests", "FR_23", new PickupRequestsScreen(me));
                add("Bins: Place & Empty", "FR_09  FR_11  FR_17", new BinsWorkScreen(me));
                add("Send a Report", "FR_20", new ReportsScreen(me));
                add("Notifications", "FR_05", inbox);
                break;
            case FINANCE_OFFICER:
                add("Billing & Pricing", "FR_13  FR_28  FR_29", new BillingScreen(me));
                add("Fraud Detection", "FR_31", new FraudScreen(me));
                add("Disputes & Refunds", "FR_34", new DisputesScreen(me));
                add("Expenses & Tax", "FR_32  FR_33", new ExpensesScreen(me));
                add("Budget Forecast", "FR_35", new BudgetScreen(me));
                add("Notifications", "alerts", inbox);
                break;
            case RECYCLING_OPERATOR:
                add("Recycling Plant", "FR_17  FR_18  FR_19", new RecyclingScreen(me));
                add("Send a Report", "FR_20", new ReportsScreen(me));
                add("Notifications", "alerts", inbox);
                break;
            default:
                add("My Profile", "FR_10  FR_21", new ProfileScreen(me));
                add("Throw Waste (scan bin)", "FR_08  FR_12  FR_14", new DisposeScreen(me));
                add("Which Bin? (photo AI)", "FR_22", new ClassifyScreen(me));
                add("Request a Pickup", "FR_23", new RequestPickupScreen(me));
                add("Rewards & Leaderboard", "FR_24  FR_25  FR_26", new RewardsScreen(me));
                add("Donate Items", "FR_27", new DonateScreen(me));
                add("My Bills", "FR_29  FR_34", new MyBillsScreen(me));
                add("Events & Programs", "FR_15  FR_16", new ProgramsScreen(me));
                add("Send a Report", "FR_20", new ReportsScreen(me));
                add("Notifications", "FR_21", inbox);
        }
        section("About the code");
        add("Design Patterns", "Singleton Observer Factory Decorator", new PatternsScreen(me));
        menu.add(Box.createVerticalGlue());
    }

    private void section(String title) {
        JLabel l = Theme.label(title.toUpperCase(), Theme.SMALL.deriveFont(java.awt.Font.BOLD), Theme.MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(14, 10, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(l);
    }

    private void add(String name, String frs, Screen screen) {
        NavItem item = new NavItem(name, frs, screen);
        items.add(item);
        menu.add(item);
        content.add(screen, name);
    }

    private void open(Screen screen) {
        current = screen;
        for (NavItem i : items) i.setActive(i.screen == screen);
        for (NavItem i : items) if (i.screen == screen) cards.show(content, i.name);
        try {
            screen.refresh();
        } catch (RuntimeException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage());
        }
        if (screen == inbox) seenNotifications = me.getNotifications().size();
        updateBell();
    }

    public void onTick() {
        if (current != null) current.onTick();
        updateBell();
    }

    private void updateBell() {
        int unread = Math.max(0, me.getNotifications().size() - seenNotifications);
        bell.setText(unread > 0 ? "Notifications (" + unread + " new)" : "Notifications");
        bell.setColor(unread > 0 ? new Color(0xF9A825) : new Color(0x388E3C));
    }

    /** One entry of the menu. */
    private class NavItem extends JPanel {
        final String name;
        final Screen screen;
        private boolean active;

        NavItem(String name, String frs, Screen screen) {
            super(new BorderLayout());
            this.name = name;
            this.screen = screen;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 8));
            add(Theme.label(name, Theme.BOLD, Theme.TEXT), BorderLayout.NORTH);
            add(Theme.label(frs, Theme.SMALL, Theme.MUTED), BorderLayout.SOUTH);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) { open(screen); }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.smooth(g2);
                g2.setColor(Theme.GREEN_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Theme.GREEN);
                g2.fillRoundRect(0, 6, 4, getHeight() - 12, 4, 4);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
