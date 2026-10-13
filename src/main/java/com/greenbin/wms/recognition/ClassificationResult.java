package com.greenbin.wms.recognition;

import com.greenbin.wms.model.WasteType;

/** FR_22: what the citizen sees after uploading a photo. */
public class ClassificationResult {
    private final WasteType type;
    private final double confidence;

    public ClassificationResult(WasteType type, double confidence) {
        this.type = type;
        this.confidence = confidence;
    }

    public WasteType getType() { return type; }
    public double getConfidence() { return confidence; }
    public boolean isRecognized() { return type != null; }
    public boolean isRecyclable() { return type != null && type.isRecyclable(); }
    public boolean isHazardous() { return type != null && type.isHazardous(); }

    /** Extra advice for the item (step 3 of FR_22). */
    public String getAdvice() {
        if (type == null) return "We could not recognise this item. Try another photo or a clearer name.";
        switch (type) {
            case PAPER: return "Flatten boxes and keep paper dry. Greasy pizza boxes go to organic.";
            case PLASTIC: return "Empty and rinse the item. Remove the cap if it is a different material.";
            case METAL_AND_GLASS: return "Rinse cans and jars. Do not put mirrors or ceramics in this bin.";
            case ORGANIC_WASTE: return "Food and garden waste only, no plastic bags. It becomes compost.";
            case HAZARDOUS: return "Do NOT throw it in a normal bin. Request a special pickup or use a hazardous bin.";
            default: return "";
        }
    }
}
