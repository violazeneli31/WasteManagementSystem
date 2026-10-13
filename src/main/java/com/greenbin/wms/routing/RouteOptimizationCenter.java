package com.greenbin.wms.routing;

import com.greenbin.wms.collection.Route;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.model.Coordinate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SINGLETON PATTERN - there is only one route optimizer in the whole system, so two parts
 * of the program can never calculate different routes for the same trucks.
 * It also remembers which route type (IRouteOptimizer) is used now.
 */
public final class RouteOptimizationCenter {
    private static RouteOptimizationCenter instance;

    private IRouteOptimizer strategy = new NormalRoutineRoute();
    private int routesCalculated;

    private RouteOptimizationCenter() { }          // nobody else can create one

    public static synchronized RouteOptimizationCenter getInstance() {
        if (instance == null) instance = new RouteOptimizationCenter();
        return instance;
    }

    public void setStrategy(IRouteOptimizer strategy) { this.strategy = strategy; }
    public IRouteOptimizer getStrategy() { return strategy; }

    /** Picks the right strategy for today's conditions. */
    public IRouteOptimizer chooseStrategy(RouteConditions conditions) {
        if (!conditions.getPriority().isEmpty()) return new SpecialPickupsRoute();
        if (conditions.isSevereWeather() || !conditions.getBlocked().isEmpty()) return new WeatherBasedRoute();
        return new NormalRoutineRoute();
    }

    /** Builds a route with the current strategy. */
    public Route buildRoute(LocalDate date, ZoneLocation start, List<ZoneLocation> stops, RouteConditions conditions) {
        List<ZoneLocation> ordered = strategy.getOptimizedRoute(start, stops, conditions);
        List<ZoneLocation> skipped = new ArrayList<>();
        for (ZoneLocation s : stops) if (!ordered.contains(s)) skipped.add(s);
        double distance = strategy.getRouteTotalDistance(start, ordered);
        routesCalculated++;
        return new Route(date, start, ordered, skipped, strategy.getName(), distance,
                strategy.getRouteEstimatedTime(distance, ordered.size()));
    }

    /** FR_04 step 6: the closest truck to a special pickup. */
    public Vehicle findNearestVehicle(Coordinate target, List<Vehicle> vehicles) {
        Vehicle best = null;
        for (Vehicle v : vehicles) {
            if (best == null || v.getCurrentLocation().distanceKm(target) < best.getCurrentLocation().distanceKm(target)) {
                best = v;
            }
        }
        return best;
    }

    public int getRoutesCalculated() { return routesCalculated; }
}
