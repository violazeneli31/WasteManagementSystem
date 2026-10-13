package com.greenbin.wms.notification;

/** DECORATOR PATTERN - adds the reward points earned to any notification. */
public class RewardNotification extends NotificationDecorator {
    private final int points;
    private final int totalPoints;

    public RewardNotification(INotification wrapped, int points, int totalPoints) {
        super(wrapped);
        this.points = points;
        this.totalPoints = totalPoints;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + "\n[Reward] +" + points + " points (you now have " + totalPoints + ")";
    }

    public int getPoints() { return points; }
}
