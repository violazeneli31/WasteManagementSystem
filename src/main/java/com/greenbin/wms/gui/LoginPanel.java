package com.greenbin.wms.gui;

import com.greenbin.wms.people.Account;
import com.greenbin.wms.system.DemoData;
import com.greenbin.wms.system.GreenBinSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** Login with username + password; managers and finance officers also need a 2FA code (NFR_14). */
public class LoginPanel extends JPanel {
    private final Consumer<Account> onLogin;
    private final JTextField username = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);
    private final JLabel error = Theme.label(" ", Theme.SMALL, Theme.RED);

    public LoginPanel(Consumer<Account> onLogin) {
        super(new BorderLayout());
        this.onLogin = onLogin;
        add(new Banner(), BorderLayout.WEST);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.BG);
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), Theme.padding(24)));
        form.add(left(Theme.label("Log in", Theme.H1, Theme.TEXT)));
        form.add(left(Theme.label("Use your GreenBin account", Theme.NORMAL, Theme.MUTED)));
        form.add(Box.createVerticalStrut(16));
        form.add(left(Theme.label("Username", Theme.BOLD, Theme.TEXT)));
        form.add(left(username));
        form.add(Box.createVerticalStrut(8));
        form.add(left(Theme.label("Password", Theme.BOLD, Theme.TEXT)));
        form.add(left(password));
        form.add(Box.createVerticalStrut(6));
        form.add(left(error));
        form.add(Box.createVerticalStrut(8));
        FlatButton login = FlatButton.primary("Log in", e -> tryLogin());
        form.add(left(login));
        username.addActionListener(e -> password.requestFocus());
        password.addActionListener(e -> tryLogin());

        box.add(form);
        box.add(Box.createVerticalStrut(18));
        box.add(left(Theme.label("Demo accounts - click one to log in (password: " + DemoData.PASSWORD + ")", Theme.BOLD, Theme.TEXT)));
        box.add(Box.createVerticalStrut(8));
        JPanel demos = new JPanel(new GridLayout(0, 2, 8, 8));
        demos.setOpaque(false);
        demos.add(demo("viola", "Viola Zeneli", "Company Manager", Theme.GREEN));
        demos.add(demo("ardi", "Ardi Hoxha", "Garbage Collector", Theme.ORANGE));
        demos.add(demo("ana", "Ana Marku", "Citizen", Theme.BLUE));
        demos.add(demo("school21", "School 21", "Institution", Theme.BLUE));
        demos.add(demo("mira", "Mira Dervishi", "Finance Officer", Theme.PURPLE));
        demos.add(demo("toni", "Toni Gjoka", "Recycling Operator", new Color(0x00897B)));
        demos.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(demos);
        right.add(box);
        add(right, BorderLayout.CENTER);
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JTextField) c.setMaximumSize(new Dimension(320, 32));
        return c;
    }

    private JPanel demo(String user, String name, String role, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), Theme.padding(8))));
        p.add(Theme.label(name, Theme.BOLD, Theme.TEXT), BorderLayout.NORTH);
        p.add(Theme.label(role + "  -  " + user, Theme.SMALL, Theme.MUTED), BorderLayout.SOUTH);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setPreferredSize(new Dimension(220, 52));
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                username.setText(user);
                password.setText(DemoData.PASSWORD);
                tryLogin();
            }
        });
        return p;
    }

    private void tryLogin() {
        GreenBinSystem sys = GreenBinSystem.getInstance();
        Account a = sys.login(username.getText().trim(), new String(password.getPassword()));
        if (a == null) {
            error.setText("Wrong username or password (or the account is blocked).");
            return;
        }
        if (sys.needsTwoFactor(a)) {
            String code = sys.getTwoFactorAuth().sendCode();
            String typed = (String) JOptionPane.showInputDialog(this,
                    "Two-factor authentication\nA 6-digit code was sent to your phone.\n\n(Demo: the code is " + code + ")",
                    "Confirm it's you", JOptionPane.PLAIN_MESSAGE, null, null, code);
            if (typed == null) return;
            if (!sys.getTwoFactorAuth().verify(typed)) {
                error.setText("The code is not correct. Try again.");
                return;
            }
        }
        onLogin.accept(a);
    }

    /** The green left side with the logo and a short description. */
    private static class Banner extends JPanel {
        Banner() {
            setPreferredSize(new Dimension(470, 100));
            setLayout(new BorderLayout());
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setBorder(BorderFactory.createEmptyBorder(250, 44, 40, 40));
            text.add(Theme.label("GreenBin Albania", new java.awt.Font(Theme.FONT, java.awt.Font.BOLD, 34), Color.WHITE));
            text.add(Theme.label("Waste Management System", Theme.H2.deriveFont(18f), new Color(0xC8E6C9)));
            text.add(Box.createVerticalStrut(24));
            String[] lines = {"Smart bins with sensors and barcodes", "Schedules from waste statistics",
                    "Optimized routes and live truck tracking", "Rewards, vouchers and community events",
                    "Recycling plant tracking", "Billing, fraud checks and forecasts"};
            for (String l : lines) {
                text.add(Theme.label("✓  " + l, Theme.NORMAL.deriveFont(14f), Color.WHITE));
                text.add(Box.createVerticalStrut(6));
            }
            add(text, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            g.setPaint(new GradientPaint(0, 0, new Color(0x2E7D32), 0, getHeight(), new Color(0x1B5E20)));
            g.fillRect(0, 0, getWidth(), getHeight());
            Logo.paint(g, 44, 70, 140);
            g.setColor(new Color(255, 255, 255, 18));
            g.fillOval(getWidth() - 220, getHeight() - 220, 360, 360);
            g.dispose();
        }
    }

    /** The GreenBin logo: a green bin with a leaf. Also used in the header. */
    public static final class Logo {
        private Logo() { }

        public static void paint(Graphics2D g, int x, int y, int size) {
            double s = size / 100.0;
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, size, size, (int) (30 * s), (int) (30 * s));
            g.setColor(Theme.GREEN);
            g.fillRoundRect(x + (int) (22 * s), y + (int) (22 * s), (int) (56 * s), (int) (8 * s), 6, 6);
            g.fillRect(x + (int) (42 * s), y + (int) (16 * s), (int) (16 * s), (int) (6 * s));
            int[] px = {x + (int) (26 * s), x + (int) (74 * s), x + (int) (68 * s), x + (int) (32 * s)};
            int[] py = {y + (int) (34 * s), y + (int) (34 * s), y + (int) (84 * s), y + (int) (84 * s)};
            g.fillPolygon(px, py, 4);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke((float) (4 * s)));
            g.drawLine(x + (int) (50 * s), y + (int) (74 * s), x + (int) (50 * s), y + (int) (50 * s));
            g.fillOval(x + (int) (38 * s), y + (int) (40 * s), (int) (22 * s), (int) (14 * s));
        }
    }
}
