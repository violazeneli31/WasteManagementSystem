package com.greenbin.wms.collection;

import com.greenbin.wms.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * FR_01 Working Zone Map: the service territory with pickup points, institutions and recycling sites.
 * New locations are validated: inside the zone, with a name, and not a duplicate (closer than 10 m).
 */
public class WorkingZoneMap {
    public static final double DUPLICATE_DISTANCE_METERS = 10;

    private final String zoneName;
    private final double minLat, maxLat, minLon, maxLon;
    private final List<ZoneLocation> locations = new ArrayList<>();

    public WorkingZoneMap(String zoneName, double minLat, double maxLat, double minLon, double maxLon) {
        this.zoneName = zoneName;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    /** Returns null if the location can be added, otherwise the reason why not. */
    public String validate(ZoneLocation location) {
        if (location.getName() == null || location.getName().isBlank()) return "Please give the location a name.";
        if (!isInsideZone(location.getCoordinate())) return "This point is outside the " + zoneName + " zone.";
        for (ZoneLocation existing : locations) {
            if (existing.getName().equalsIgnoreCase(location.getName().trim())) {
                return "A location called '" + existing.getName() + "' already exists.";
            }
            if (existing.getCoordinate().distanceMeters(location.getCoordinate()) < DUPLICATE_DISTANCE_METERS) {
                return "Duplicate: '" + existing.getName() + "' is less than 10 m away.";
            }
        }
        return null;
    }

    public boolean addLocation(ZoneLocation location) {
        if (validate(location) != null) return false;
        locations.add(location);
        return true;
    }

    public boolean removeLocation(ZoneLocation location) {
        return locations.remove(location);
    }

    public boolean removeLocation(Coordinate coords) {
        return locations.removeIf(l -> l.getCoordinate().equals(coords));
    }

    public boolean isInsideZone(Coordinate c) {
        return c.getLatitude() >= minLat && c.getLatitude() <= maxLat
                && c.getLongitude() >= minLon && c.getLongitude() <= maxLon;
    }

    /** The closest location to a point, or null if the map is empty. */
    public ZoneLocation findNearest(Coordinate c) {
        ZoneLocation best = null;
        for (ZoneLocation l : locations) {
            if (best == null || l.getCoordinate().distanceKm(c) < best.getCoordinate().distanceKm(c)) best = l;
        }
        return best;
    }

    public ZoneLocation findByName(String name) {
        for (ZoneLocation l : locations) if (l.getName().equalsIgnoreCase(name)) return l;
        return null;
    }

    public List<ZoneLocation> getLocations(LocationType type) {
        List<ZoneLocation> result = new ArrayList<>();
        for (ZoneLocation l : locations) if (l.getType() == type) result.add(l);
        return result;
    }

    public List<ZoneLocation> getLocations() { return locations; }
    public String getZoneName() { return zoneName; }
    public double getMinLat() { return minLat; }
    public double getMaxLat() { return maxLat; }
    public double getMinLon() { return minLon; }
    public double getMaxLon() { return maxLon; }
}
