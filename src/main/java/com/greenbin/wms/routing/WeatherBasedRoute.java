package com.greenbin.wms.routing;

import com.greenbin.wms.collection.ZoneLocation;

import java.util.ArrayList;
import java.util.List;

/** Route type for bad weather or blocked roads: skips the blocked stops and drives slower. */
public class WeatherBasedRoute implements IRouteOptimizer {

    @Override
    public List<ZoneLocation> getOptimizedRoute(ZoneLocation start, List<ZoneLocation> stops, RouteConditions conditions) {
        List<ZoneLocation> reachable = new ArrayList<>();
        for (ZoneLocation s : stops) {
            if (!conditions.getBlocked().contains(s)) reachable.add(s);
        }
        return NearestNeighbour.order(start, reachable);
    }

    @Override public String getName() { return "Weather / blocked roads"; }
    @Override public String getDescription() { return "Storm or road blockage: avoids blocked stops and plans at a lower speed."; }
    @Override public double getAverageSpeedKmh() { return 18; }
}
