package com.greenbin.wms.people;

import com.greenbin.wms.bins.DisposalRecord;
import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.community.BusinessVoucher;
import com.greenbin.wms.community.Report;
import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.model.Role;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.notification.NotificationCategory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A user of the system (citizen or institution). Also an OBSERVER: it receives notifications.
 * FR_10 profile, FR_12 points, FR_21 notification preferences.
 */
public class User implements Account {
    private final UUID userID = UUID.randomUUID();
    private String username;
    private String passwordHash;
    private String phoneNumber;
    private String email;
    private String address;
    private String community;           // neighbourhood used for the community leaderboard (FR_24)
    private int points;
    private boolean blocked;            // set when fraud is confirmed (FR_31)

    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Bill> bills = new ArrayList<>();
    private final List<Report> reports = new ArrayList<>();
    private final List<BusinessVoucher> vouchers = new ArrayList<>();
    private final List<DisposalRecord> disposalHistory = new ArrayList<>();
    private final List<String> badges = new ArrayList<>();
    private final List<INotification> informativeNotifications = new ArrayList<>();
    private final Map<NotificationCategory, Boolean> notificationPreferences = new EnumMap<>(NotificationCategory.class);

    public User(String username, String password, String email, String phoneNumber, String address, String community) {
        this.username = username;
        this.passwordHash = PasswordHasher.hash(password);
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.community = community;
        for (NotificationCategory c : NotificationCategory.values()) notificationPreferences.put(c, true);
    }

    // ---------- login & profile (FR_10) ----------

    @Override
    public boolean login(String username, String password) {
        return !blocked && this.username.equals(username) && PasswordHasher.matches(password, passwordHash);
    }

    /** Changes the profile. Empty values are ignored. */
    public void updateProfile(String newUsername, String newPhone, String newEmail, String newAddress) {
        if (notEmpty(newUsername)) username = newUsername.trim();
        if (notEmpty(newPhone)) phoneNumber = newPhone.trim();
        if (notEmpty(newEmail)) {
            if (!newEmail.contains("@")) throw new IllegalArgumentException("The email address is not valid");
            email = newEmail.trim();
        }
        if (notEmpty(newAddress)) address = newAddress.trim();
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        if (!PasswordHasher.matches(oldPassword, passwordHash)) return false;
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("The new password needs at least 6 characters");
        }
        passwordHash = PasswordHasher.hash(newPassword);
        return true;
    }

    private static boolean notEmpty(String s) { return s != null && !s.isBlank(); }

    // ---------- points (FR_12) ----------

    public void addPoints(int amount) { points += amount; }

    /** Removes points (vouchers, corrections). Returns false if there are not enough. */
    public boolean usePoints(int amount) {
        if (amount > points) return false;
        points -= amount;
        return true;
    }

    // ---------- notifications: OBSERVER + preferences (FR_21) ----------

    @Override
    public void update(INotification notification) {
        informativeNotifications.add(0, notification);
    }

    @Override
    public boolean wantsNotification(NotificationCategory category) {
        return notificationPreferences.getOrDefault(category, true);
    }

    public void selectNotifications(Map<NotificationCategory, Boolean> choices) {
        notificationPreferences.putAll(choices);
    }

    public void setNotificationPreference(NotificationCategory category, boolean enabled) {
        notificationPreferences.put(category, enabled);
    }

    @Override public List<INotification> getNotifications() { return informativeNotifications; }
    @Override public String getObserverName() { return getDisplayName(); }

    // ---------- getters ----------

    public UUID getUserID() { return userID; }
    @Override public String getUsername() { return username; }
    @Override public String getDisplayName() { return username; }
    @Override public Role getRole() { return Role.USER; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getCommunity() { return community; }
    public int getPoints() { return points; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public List<Pickup> getPickups() { return pickups; }
    public List<Bill> getBills() { return bills; }
    public List<Report> getReports() { return reports; }
    public List<BusinessVoucher> getVouchers() { return vouchers; }
    public List<DisposalRecord> getDisposalHistory() { return disposalHistory; }
    public List<String> getBadges() { return badges; }
    public Map<NotificationCategory, Boolean> getNotificationPreferences() { return notificationPreferences; }

    /** Email shown with the middle hidden, so personal data is protected on screen. */
    public String getMaskedEmail() {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    public String toString() { return getDisplayName(); }
}
