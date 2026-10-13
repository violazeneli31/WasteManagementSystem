package com.greenbin.wms.routing;

import com.greenbin.wms.collection.ZoneLocation;

import java.util.List;

/** Route type for normal days: visit every stop, closest first. */
public class NormalRoutineRoute implements IRouteOptimizer {

    @Override
    public List<ZoneLocation> getOptimizedRoute(ZoneLocation start, List<ZoneLocation> stops, RouteConditions conditions) {
        return NearestNeighbour.order(start, stops);
    }

    @Override public String getName() { return "Normal routine"; }
    @Override public String getDescription() { return "Normal conditions: visits every stop, always going to the closest one next."; }
    @Override public double getAverageSpeedKmh() { return 30; }
}
