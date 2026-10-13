package com.greenbin.wms.community;

import com.greenbin.wms.model.WasteType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FR_26: a third-party app connected to the system that provides advertisements.
 * Ads are shown to citizens based on their activity (the waste type they recycle the most).
 */
public class App {
    /** One advertisement. targetType == null means it is shown to everyone. */
    public static class Advertisement {
        private final String title;
        private final String text;
        private final WasteType targetType;
        private int impressions;
        private int clicks;

        public Advertisement(String title, String text, WasteType targetType) {
            this.title = title;
            this.text = text;
            this.targetType = targetType;
        }

        public void show() { impressions++; }
        public void click() { clicks++; }

        public String getTitle() { return title; }
        public String getText() { return text; }
        public WasteType getTargetType() { return targetType; }
        public int getImpressions() { return impressions; }
        public int getClicks() { return clicks; }
        public double getClickRate() { return impressions == 0 ? 0 : 100.0 * clicks / impressions; }
    }

    private final UUID appID = UUID.randomUUID();
    private final String appName;
    private boolean status;
    private final List<Advertisement> ads = new ArrayList<>();

    public App(String appName, boolean enabled) {
        this.appName = appName;
        this.status = enabled;
    }

    public void enableApp() { status = true; }
    public void disableApp() { status = false; }

    public void addAd(Advertisement ad) { ads.add(ad); }

    /** Ads that match the citizen's favourite waste type, plus general ones. Nothing if the app is off. */
    public List<Advertisement> getAdsFor(WasteType favouriteType) {
        List<Advertisement> result = new ArrayList<>();
        if (!status) return result;
        for (Advertisement ad : ads) {
            if (ad.getTargetType() == null || ad.getTargetType() == favouriteType) result.add(ad);
        }
        return result;
    }

    public int getTotalImpressions() {
        int sum = 0;
        for (Advertisement a : ads) sum += a.getImpressions();
        return sum;
    }

    public int getTotalClicks() {
        int sum = 0;
        for (Advertisement a : ads) sum += a.getClicks();
        return sum;
    }

    public UUID getAppID() { return appID; }
    public String getAppName() { return appName; }
    public boolean isEnabled() { return status; }
    public List<Advertisement> getAds() { return ads; }

    @Override
    public String toString() { return appName; }
}
