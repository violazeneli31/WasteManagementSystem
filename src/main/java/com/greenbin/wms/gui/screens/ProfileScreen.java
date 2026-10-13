package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.DisposalRecord;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.notification.NotificationCategory;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/** FR_10 citizen profile and FR_21 choosing which notifications to receive. */
public class ProfileScreen extends Screen {
    private final User user;
    private final JLabel points = Theme.label("0", Theme.H1.deriveFont(40f), Theme.GREEN);
    private final JLabel info = new JLabel();
    private final JTextField username = new JTextField();
    private final JTextField phone = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField address = new JTextField();
    private final JPasswordField oldPass = new JPasswordField();
    private final JPasswordField newPass = new JPasswordField();
    private final Map<NotificationCategory, JCheckBox> prefs = new EnumMap<>(NotificationCategory.class);
    private final DefaultTableModel history = Tables.model("Date", "Bin", "Waste", "Weight", "Right bin?", "Points");

    public ProfileScreen(Account me) {
        super(me, "My Profile", "Your points, your details and the notifications you want", "FR_10", "FR_21");
        user = (User) me;

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(380, 100));
        JPanel summary = new JPanel(new BorderLayout());
        summary.setOpaque(false);
        summary.add(points, BorderLayout.NORTH);
        info.setFont(Theme.NORMAL);
        summary.add(info, BorderLayout.CENTER);
        left.add(Theme.card("Reward points", summary), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.setOpaque(false);
        form.add(muted("Username"));
        form.add(username);
        form.add(muted("Phone"));
        form.add(phone);
        form.add(muted("Email"));
        form.add(email);
        form.add(muted("Address"));
        form.add(address);
        form.add(new JLabel());
        form.add(FlatButton.primary("Save profile", e -> saveProfile()));
        form.add(muted("Current password"));
        form.add(oldPass);
        form.add(muted("New password"));
        form.add(newPass);
        form.add(new JLabel());
        form.add(FlatButton.secondary("Change password", e -> changePassword()));
        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.add(Theme.card("Edit profile (stored securely, password hashed)", form), BorderLayout.NORTH);
        left.add(formWrap, BorderLayout.CENTER);
        body().add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        JPanel checks = new JPanel(new GridLayout(0, 3, 6, 4));
        checks.setOpaque(false);
        for (NotificationCategory c : NotificationCategory.values()) {
            JCheckBox cb = new JCheckBox(c.getLabel());
            cb.setOpaque(false);
            prefs.put(c, cb);
            checks.add(cb);
        }
        JPanel prefPanel = new JPanel(new BorderLayout(0, 6));
        prefPanel.setOpaque(false);
        prefPanel.add(checks, BorderLayout.CENTER);
        prefPanel.add(row(FlatButton.primary("Save notification settings", e -> savePrefs()),
                muted("Turned-off categories are not delivered to you (Observer checks before sending).")), BorderLayout.SOUTH);
        center.add(Theme.card("Settings > Notifications (FR_21)", prefPanel), BorderLayout.NORTH);
        center.add(Theme.card("My waste disposal history", Theme.table(Theme.newTable(history))), BorderLayout.CENTER);
        body().add(center, BorderLayout.CENTER);
    }

    private void saveProfile() {
        try {
            user.updateProfile(username.getText(), phone.getText(), email.getText(), address.getText());
            info("Profile saved.");
            refresh();
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void changePassword() {
        try {
            if (user.changePassword(new String(oldPass.getPassword()), new String(newPass.getPassword()))) info("Password changed.");
            else error("The current password is wrong.");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
        oldPass.setText("");
        newPass.setText("");
    }

    private void savePrefs() {
        Map<NotificationCategory, Boolean> choice = new EnumMap<>(NotificationCategory.class);
        for (Map.Entry<NotificationCategory, JCheckBox> e : prefs.entrySet()) choice.put(e.getKey(), e.getValue().isSelected());
        user.selectNotifications(choice);
        info("Notification settings saved.");
    }

    @Override
    public void refresh() {
        points.setText(user.getPoints() + " pts");
        info.setText("<html><b>" + user.getDisplayName() + "</b> (" + user.getUsername() + ")<br>" + user.getMaskedEmail()
                + " &nbsp; " + user.getPhoneNumber() + "<br>" + user.getAddress() + "<br>Community: " + user.getCommunity()
                + (user.getBadges().isEmpty() ? "" : "<br><font color='#2E7D32'><b>Badges: " + String.join(", ", user.getBadges()) + "</b></font>")
                + "</html>");
        username.setText(user.getUsername());
        phone.setText(user.getPhoneNumber());
        email.setText(user.getEmail());
        address.setText(user.getAddress());
        for (Map.Entry<NotificationCategory, JCheckBox> e : prefs.entrySet()) e.getValue().setSelected(user.wantsNotification(e.getKey()));
        history.setRowCount(0);
        for (int i = user.getDisposalHistory().size() - 1; i >= 0; i--) {
            DisposalRecord r = user.getDisposalHistory().get(i);
            history.addRow(new Object[]{r.getTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), r.getBin().toString(),
                    r.getWaste().getType(), Tables.kg(r.getWaste().getWeight()), r.isCorrect() ? "yes" : "NO", "+" + r.getPoints()});
        }
    }
}
