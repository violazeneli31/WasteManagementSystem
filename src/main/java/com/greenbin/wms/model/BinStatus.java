package com.greenbin.wms.model;

/** Status of a smart bin: ACTIVE (under 70%), WARNING (70-89%), FULL (90% or more). */
public enum BinStatus {
    ACTIVE, WARNING, FULL;

    public static BinStatus fromPercent(double percent) {
        if (percent >= 90) return FULL;
        if (percent >= 70) return WARNING;
        return ACTIVE;
    }
}
