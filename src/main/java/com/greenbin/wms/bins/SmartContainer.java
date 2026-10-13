package com.greenbin.wms.bins;

import com.greenbin.wms.model.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Anything with a sensor and a barcode: smart bins and donation containers (FR_08, FR_09).
 */
public abstract class SmartContainer {
    public static final double FULL_PERCENT = 90;

    private final UUID containerID = UUID.randomUUID();
    private final String location;
    private final Coordinate coordinate;
    private final double capacity;           // kg
    private double currentFillLevel;         // kg
    private final Barcode barcode;
    private final Sensor sensor = new Sensor();
    private final List<Double> fillHistory = new ArrayList<>();   // FR_09 step 6
    private boolean alertSent;

    protected SmartContainer(String location, Coordinate coordinate, double capacity, long barcodeSerial) {
        this.location = location;
        this.coordinate = coordinate;
        this.capacity = capacity;
        this.barcode = new Barcode(barcodeSerial);
        fillHistory.add(0.0);
    }

    public void updateFillLevel(double kg) {
        currentFillLevel = Math.max(0, Math.min(capacity, currentFillLevel + kg));
        fillHistory.add(getFillPercent());
    }

    /** The real fill level in percent. */
    public double getFillPercent() { return 100.0 * currentFillLevel / capacity; }

    /** What the sensor reports: -1 if the sensor is broken. */
    public double readSensor() { return sensor.isWorking() ? getFillPercent() : -1; }

    public boolean checkIfFull() { return getFillPercent() >= FULL_PERCENT; }

    /** Emptied by the garbage collectors. */
    public void resetContainer() {
        currentFillLevel = 0;
        alertSent = false;
        fillHistory.add(0.0);
    }

    public abstract String getKind();

    public UUID getContainerID() { return containerID; }
    public String getLocation() { return location; }
    public Coordinate getCoordinate() { return coordinate; }
    public double getCapacity() { return capacity; }
    public double getCurrentFillLevel() { return currentFillLevel; }
    public Barcode getBarcode() { return barcode; }
    public Sensor getSensor() { return sensor; }
    public List<Double> getFillHistory() { return fillHistory; }
    public boolean isAlertSent() { return alertSent; }
    public void setAlertSent(boolean alertSent) { this.alertSent = alertSent; }
}
