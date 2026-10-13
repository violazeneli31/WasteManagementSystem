package com.greenbin.wms.collection;

import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.people.GarbageCollectionEmployee;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The result of route optimization (FR_04): stops in the order the truck should visit them. */
public class Route {
    private final LocalDate date;
    private final ZoneLocation start;
    private final List<ZoneLocation> orderedStops;
    private final List<ZoneLocation> skippedStops;
    private final String strategyName;
    private final double totalDistanceKm;
    private final Duration estimatedTime;
    private GarbageCollectionEmployee driver;
    private Vehicle vehicle;

    public Route(LocalDate date, ZoneLocation start, List<ZoneLocation> orderedStops, List<ZoneLocation> skippedStops,
                 String strategyName, double totalDistanceKm, Duration estimatedTime) {
        this.date = date;
        this.start = start;
        this.orderedStops = new ArrayList<>(orderedStops);
        this.skippedStops = new ArrayList<>(skippedStops);
        this.strategyName = strategyName;
        this.totalDistanceKm = totalDistanceKm;
        this.estimatedTime = estimatedTime;
    }

    /** All the points to drive through: start, every stop, and back to the start. */
    public List<Coordinate> getOptimizedRoute() {
        List<Coordinate> points = new ArrayList<>();
        points.add(start.getCoordinate());
        for (ZoneLocation s : orderedStops) points.add(s.getCoordinate());
        points.add(start.getCoordinate());
        return points;
    }

    public String showRoute() {
        StringBuilder sb = new StringBuilder(start.getName());
        for (ZoneLocation s : orderedStops) sb.append(" -> ").append(s.getName());
        return sb.append(" -> ").append(start.getName()).toString();
    }

    public LocalDate getDate() { return date; }
    public ZoneLocation getStart() { return start; }
    public List<ZoneLocation> getOrderedStops() { return orderedStops; }
    public List<ZoneLocation> getSkippedStops() { return skippedStops; }
    public String getStrategyName() { return strategyName; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public Duration getEstimatedTime() { return estimatedTime; }
    public GarbageCollectionEmployee getDriver() { return driver; }
    public void setDriver(GarbageCollectionEmployee driver) { this.driver = driver; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
}
