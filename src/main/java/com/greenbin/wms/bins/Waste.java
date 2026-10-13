package com.greenbin.wms.bins;

import com.greenbin.wms.model.WasteType;

/** A piece of waste with its real type and weight. */
public class Waste {
    private final WasteType type;
    private final double weight;
    private String recyclingStage = "In bin";
    private boolean flagged;            // thrown in the wrong bin (FR_14, FR_17)
    private WasteType binType;          // the bin it was thrown into

    public Waste(WasteType type, double weight) {
        if (weight <= 0) throw new IllegalArgumentException("Weight must be more than 0");
        this.type = type;
        this.weight = weight;
        this.binType = type;
    }

    public int calculatePoints() {
        return (int) Math.round(weight * type.getPointsPerKg());
    }

    public WasteType getType() { return type; }
    public double getWeight() { return weight; }
    public String getRecyclingStage() { return recyclingStage; }
    public void setRecyclingStage(String stage) { this.recyclingStage = stage; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
    public WasteType getBinType() { return binType; }
    public void setBinType(WasteType binType) { this.binType = binType; }

    @Override
    public String toString() { return String.format("%.1f kg %s", weight, type); }
}
