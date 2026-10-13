package com.greenbin.wms.routing;

import com.greenbin.wms.collection.ZoneLocation;

import java.util.ArrayList;
import java.util.List;

/** Route type for special pickup requests are served first, then the normal stops. */
public class SpecialPickupsRoute implements IRouteOptimizer {

    @Override
    public List<ZoneLocation> getOptimizedRoute(ZoneLocation start, List<ZoneLocation> stops, RouteConditions conditions) {
        List<ZoneLocation> urgent = new ArrayList<>();
        List<ZoneLocation> normal = new ArrayList<>();
        for (ZoneLocation s : stops) {
            if (conditions.getBlocked().contains(s)) continue;
            if (conditions.getPriority().contains(s)) urgent.add(s); else normal.add(s);
        }
        List<ZoneLocation> ordered = NearestNeighbour.order(start, urgent);
        ZoneLocation last = ordered.isEmpty() ? start : ordered.get(ordered.size() - 1);
        ordered.addAll(NearestNeighbour.order(last, normal));
        return ordered;
    }

    @Override public String getName() { return "Special pickups first"; }
    @Override public String getDescription() { return "Special requests: serves the priority stops first, then the rest."; }
    @Override public double getAverageSpeedKmh() { return 28; }
}
