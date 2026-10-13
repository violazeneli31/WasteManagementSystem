package com.greenbin.wms.collection;

import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;

import java.util.EnumSet;
import java.util.Set;

/** A marker on the working zone map (FR_01). */
public class ZoneLocation {
    private final String name;
    private final LocationType type;
    private final Coordinate coordinate;
    private final Set<WasteType> wasteCategories;

    public ZoneLocation(String name, LocationType type, Coordinate coordinate, Set<WasteType> wasteCategories) {
        this.name = name;
        this.type = type;
        this.coordinate = coordinate;
        this.wasteCategories = wasteCategories.isEmpty() ? EnumSet.noneOf(WasteType.class) : EnumSet.copyOf(wasteCategories);
    }

    public ZoneLocation(String name, LocationType type, Coordinate coordinate) {
        this(name, type, coordinate, EnumSet.allOf(WasteType.class));
    }

    public String getName() { return name; }
    public LocationType getType() { return type; }
    public Coordinate getCoordinate() { return coordinate; }
    public Set<WasteType> getWasteCategories() { return wasteCategories; }

    @Override
    public String toString() { return name; }
}
