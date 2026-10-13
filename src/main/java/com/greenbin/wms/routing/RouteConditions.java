package com.greenbin.wms.routing;

import com.greenbin.wms.collection.ZoneLocation;

import java.util.HashSet;
import java.util.Set;

/** What is happening on the roads today: blocked places, special pickups, bad weather. */
public class RouteConditions {
    private final Set<ZoneLocation> blocked = new HashSet<>();
    private final Set<ZoneLocation> priority = new HashSet<>();
    private boolean severeWeather;

    public Set<ZoneLocation> getBlocked() { return blocked; }
    public Set<ZoneLocation> getPriority() { return priority; }
    public boolean isSevereWeather() { return severeWeather; }
    public void setSevereWeather(boolean severeWeather) { this.severeWeather = severeWeather; }
}
