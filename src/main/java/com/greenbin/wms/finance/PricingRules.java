package com.greenbin.wms.finance;

import com.greenbin.wms.model.WasteType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FR_13 Dynamic pricing: price per kg for each waste type plus a fixed fee for special pickups.
 * The finance officer can change prices; every change is logged for auditing.
 */
public class PricingRules {
    private final Map<WasteType, Double> pricePerKg = new EnumMap<>(WasteType.class);
    private double specialPickupFee = 300;           // Lek
    private final List<String> changeLog = new ArrayList<>();

    public PricingRules() {
        pricePerKg.put(WasteType.PAPER, 20.0);
        pricePerKg.put(WasteType.METAL_AND_GLASS, 30.0);
        pricePerKg.put(WasteType.ORGANIC_WASTE, 15.0);
        pricePerKg.put(WasteType.PLASTIC, 25.0);
        pricePerKg.put(WasteType.HAZARDOUS, 120.0);
    }

    /** Price (without tax) of a pickup: fixed fee + weight x price per kg. */
    public double calculateCost(WasteType type, double weightKg) {
        return Math.round((specialPickupFee + weightKg * pricePerKg.get(type)) * 100) / 100.0;
    }

    public void setPrice(WasteType type, double newPrice, String changedBy) {
        if (newPrice <= 0) throw new IllegalArgumentException("Price must be more than 0");
        double old = pricePerKg.put(type, newPrice);
        changeLog.add(0, now() + "  " + changedBy + " changed " + type + ": " + old + " -> " + newPrice + " Lek/kg");
    }

    public void setSpecialPickupFee(double fee, String changedBy) {
        if (fee < 0) throw new IllegalArgumentException("Fee can't be negative");
        changeLog.add(0, now() + "  " + changedBy + " changed pickup fee: " + specialPickupFee + " -> " + fee + " Lek");
        specialPickupFee = fee;
    }

    private static String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")); }

    public double getPrice(WasteType type) { return pricePerKg.get(type); }
    public double getSpecialPickupFee() { return specialPickupFee; }
    public List<String> getChangeLog() { return changeLog; }
}
