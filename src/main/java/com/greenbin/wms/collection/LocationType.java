package com.greenbin.wms.collection;

public enum LocationType {
    PICKUP_POINT("Pickup point"),
    INSTITUTION("Institution"),
    RECYCLING_FACILITY("Recycling facility"),
    DEPOT("Truck depot");

    private final String label;
    LocationType(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
