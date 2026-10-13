package com.greenbin.wms;

import com.greenbin.wms.collection.LocationType;
import com.greenbin.wms.collection.WorkingZoneMap;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/** FR_01 Working Zone Map. */
class WorkingZoneMapTest {
    private final TestData d = new TestData();

    @Test
    void addsAValidLocation() {
        assertNull(d.sys.addLocation("Selita", LocationType.PICKUP_POINT, new Coordinate(41.3108, 19.8125), EnumSet.of(WasteType.PAPER)));
        assertNotNull(d.sys.getZoneMap().findByName("Selita"));
    }

    @Test
    void rejectsDuplicateCloserThan10Meters() {
        String error = d.sys.addLocation("Almost Blloku", LocationType.PICKUP_POINT, new Coordinate(41.31975, 19.81662), EnumSet.allOf(WasteType.class));
        assertNotNull(error);
        assertTrue(error.contains("Duplicate"));
    }

    @Test
    void rejectsSameNameEmptyNameAndOutsideZone() {
        assertNotNull(d.sys.addLocation("blloku", LocationType.PICKUP_POINT, new Coordinate(41.33, 19.80), EnumSet.allOf(WasteType.class)));
        assertNotNull(d.sys.addLocation(" ", LocationType.PICKUP_POINT, new Coordinate(41.33, 19.80), EnumSet.allOf(WasteType.class)));
        assertNotNull(d.sys.addLocation("Durres", LocationType.PICKUP_POINT, new Coordinate(41.32, 19.45), EnumSet.allOf(WasteType.class)));
    }

    @Test
    void removesALocationAndFindsTheNearest() {
        WorkingZoneMap map = d.sys.getZoneMap();
        ZoneLocation kombinat = map.findByName("Kombinat");
        assertEquals(kombinat, map.findNearest(new Coordinate(41.3190, 19.7840)));
        d.sys.removeLocation(kombinat);
        assertNull(map.findByName("Kombinat"));
    }
}
