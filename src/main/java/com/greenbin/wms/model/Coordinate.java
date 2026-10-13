package com.greenbin.wms.model;

/** A point on the map (latitude / longitude). */
public class Coordinate {
    private final double latitude;
    private final double longitude;

    public Coordinate(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinate");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    /** Distance in kilometres (good enough for one city). */
    public double distanceKm(Coordinate other) {
        double kmPerDegLat = 111.0;
        double kmPerDegLon = 111.0 * Math.cos(Math.toRadians(latitude));
        double dy = (other.latitude - latitude) * kmPerDegLat;
        double dx = (other.longitude - longitude) * kmPerDegLon;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distanceMeters(Coordinate other) { return distanceKm(other) * 1000; }

    /** A point part of the way (0..1) from this point to another one. Used to animate trucks. */
    public Coordinate moveTowards(Coordinate target, double fraction) {
        return new Coordinate(latitude + (target.latitude - latitude) * fraction,
                longitude + (target.longitude - longitude) * fraction);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Coordinate)) return false;
        Coordinate c = (Coordinate) o;
        return Double.compare(latitude, c.latitude) == 0 && Double.compare(longitude, c.longitude) == 0;
    }

    @Override
    public int hashCode() { return Double.hashCode(latitude) * 31 + Double.hashCode(longitude); }

    @Override
    public String toString() { return String.format("%.4f, %.4f", latitude, longitude); }
}
