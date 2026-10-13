package com.greenbin.wms.notification;

/** The kinds of notifications a user can switch on or off in their settings (FR_21). */
public enum NotificationCategory {
    SCHEDULE("Collection schedules & changes"),
    PICKUP("Pickup requests"),
    EVENTS("Events & educational programs"),
    REWARDS("Reward points"),
    BILLING("Bills & payments"),
    BIN_ALERTS("Smart bin alerts"),
    WEATHER("Weather alerts"),
    DONATIONS("Donations"),
    REPORTS("Reports & feedback");

    private final String label;
    NotificationCategory(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
