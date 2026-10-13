package com.greenbin.wms.routing;

import com.greenbin.wms.collection.ZoneLocation;

import java.time.Duration;
import java.util.List;

/**
 * FR_04: every type of route (normal, weather, special pickups) implements this interface,
 * so the RouteOptimizationCenter can swap them depending on the conditions (FR_04).
 */
public interface IRouteOptimizer {
    int MINUTES_PER_STOP = 6;

    /** Puts the stops in the order the truck should visit them. */
    List<ZoneLocation> getOptimizedRoute(ZoneLocation start, List<ZoneLocation> stops, RouteConditions conditions);

    String getName();

    String getDescription();

    /** Average truck speed for this kind of route. */
    double getAverageSpeedKmh();

    default double getRouteTotalDistance(ZoneLocation start, List<ZoneLocation> ordered) {
        double total = 0;
        ZoneLocation current = start;
        for (ZoneLocation stop : ordered) {
            total += current.getCoordinate().distanceKm(stop.getCoordinate());
            current = stop;
        }
        return total + current.getCoordinate().distanceKm(start.getCoordinate());   // back to the depot
    }

    default Duration getRouteEstimatedTime(double distanceKm, int stops) {
        long driving = Math.round(distanceKm / getAverageSpeedKmh() * 60);
        return Duration.ofMinutes(driving + (long) stops * MINUTES_PER_STOP);
    }
}
