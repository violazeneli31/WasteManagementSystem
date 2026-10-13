package com.greenbin.wms.model;

/** The five smart-bin categories (Waste_Type in the class diagram). */
public enum WasteType {
    PAPER("Paper", 2),
    METAL_AND_GLASS("Metal & Glass", 3),
    ORGANIC_WASTE("Organic", 1),
    PLASTIC("Plastic", 3),
    HAZARDOUS("Hazardous", 5);

    private final String label;
    private final int pointsPerKg;

    WasteType(String label, int pointsPerKg) {
        this.label = label;
        this.pointsPerKg = pointsPerKg;
    }

    public String getLabel() { return label; }
    public int getPointsPerKg() { return pointsPerKg; }

    public boolean isRecyclable() { return this != HAZARDOUS && this != ORGANIC_WASTE; }
    public boolean isHazardous() { return this == HAZARDOUS; }

    @Override
    public String toString() { return label; }
}
