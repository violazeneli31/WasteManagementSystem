package com.greenbin.wms.routing;

import com.greenbin.wms.collection.ZoneLocation;

import java.util.ArrayList;
import java.util.List;

/** The simple idea all strategies use: always drive to the closest stop you have not visited yet. */
final class NearestNeighbour {
    private NearestNeighbour() { }

    static List<ZoneLocation> order(ZoneLocation start, List<ZoneLocation> stops) {
        List<ZoneLocation> left = new ArrayList<>(stops);
        List<ZoneLocation> ordered = new ArrayList<>();
        ZoneLocation current = start;
        while (!left.isEmpty()) {
            ZoneLocation closest = left.get(0);
            for (ZoneLocation s : left) {
                if (current.getCoordinate().distanceKm(s.getCoordinate())
                        < current.getCoordinate().distanceKm(closest.getCoordinate())) {
                    closest = s;
                }
            }
            ordered.add(closest);
            left.remove(closest);
            current = closest;
        }
        return ordered;
    }
}
