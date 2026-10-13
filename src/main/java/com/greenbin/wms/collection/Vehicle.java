package com.greenbin.wms.collection;

import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.people.GarbageCollectionEmployee;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A garbage truck. It has a GPS position (FR_06) and drives along its route
 * a little bit every time drive() is called, which makes the map "live".
 */
public class Vehicle {
    private final UUID vehicleID = UUID.randomUUID();
    private final String plateNumber;
    private final List<GarbageCollectionEmployee> assignedEmployees = new ArrayList<>();
    private double fuelLevel;
    private Coordinate currentLocation;
    private final List<Coordinate> path = new ArrayList<>();
    private int nextPoint;
    private double speedStep = 0.25;            // how far it moves on each tick (km)
    private boolean loop;                       // demo trucks repeat their route forever

    public Vehicle(String plateNumber, double fuelLevel, Coordinate start) {
        this.plateNumber = plateNumber;
        this.fuelLevel = fuelLevel;
        this.currentLocation = start;
    }

    /** Gives the truck a route to follow. */
    public void followPath(List<Coordinate> points, boolean loop) {
        this.loop = loop;
        path.clear();
        path.addAll(points);
        nextPoint = 0;
    }

    /** Moves the truck towards the next point of its route. Called by the live timer. */
    public void drive() {
        if (nextPoint >= path.size()) {
            if (!loop || path.isEmpty()) return;
            nextPoint = 0;
        }
        if (path.get(nextPoint).equals(currentLocation)) nextPoint = (nextPoint + 1) % path.size();   // already there
        Coordinate target = path.get(nextPoint);
        double distance = currentLocation.distanceKm(target);
        if (distance <= speedStep) {
            currentLocation = target;
            nextPoint++;
        } else {
            currentLocation = currentLocation.moveTowards(target, speedStep / distance);
        }
        fuelLevel = Math.max(5, fuelLevel - 0.02);
    }

    public boolean isDriving() { return nextPoint < path.size(); }

    public String getStatusText() {
        if (!isDriving()) return "Parked";
        return "Driving (" + nextPoint + "/" + path.size() + " stops)";
    }

    public Coordinate showLiveLocation() { return currentLocation; }

    public UUID getVehicleID() { return vehicleID; }
    public String getPlateNumber() { return plateNumber; }
    public List<GarbageCollectionEmployee> getAssignedEmployees() { return assignedEmployees; }
    public double getFuelLevel() { return fuelLevel; }
    public Coordinate getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Coordinate c) { this.currentLocation = c; }
    public List<Coordinate> getPath() { return path; }
    public void setSpeedStep(double km) { this.speedStep = km; }

    @Override
    public String toString() { return plateNumber; }
}
