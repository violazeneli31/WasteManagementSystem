package com.greenbin.wms;

import com.greenbin.wms.collection.Route;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.collection.ZoneLocation;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.routing.NormalRoutineRoute;
import com.greenbin.wms.routing.RouteConditions;
import com.greenbin.wms.routing.RouteOptimizationCenter;
import com.greenbin.wms.routing.SpecialPickupsRoute;
import com.greenbin.wms.routing.WeatherBasedRoute;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FR_04 route optimization (Strategy) and the Singleton route optimizer. */
class RouteOptimizationTest {
    private final TestData d = new TestData();
    private final List<ZoneLocation> stops = d.sys.getZoneMap().getLocations(com.greenbin.wms.collection.LocationType.PICKUP_POINT);

    @Test
    void thereIsOnlyOneRouteOptimizer() {
        assertSame(RouteOptimizationCenter.getInstance(), RouteOptimizationCenter.getInstance());
    }

    @Test
    void normalRouteVisitsTheClosestStopFirst() {
        Route r = d.sys.buildRoute(LocalDate.now().with(java.time.DayOfWeek.MONDAY), stops, new RouteConditions(), new NormalRoutineRoute());
        assertEquals(4, r.getOrderedStops().size());
        assertEquals("Skanderbeg Square", r.getOrderedStops().get(0).getName());   // closest to the depot
        assertTrue(r.getTotalDistanceKm() > 0);
        assertTrue(r.getEstimatedTime().toMinutes() > 0);
    }

    @Test
    void weatherRouteSkipsBlockedRoadsAndIsSlower() {
        ZoneLocation blloku = d.sys.getZoneMap().findByName("Blloku");
        d.sys.setBlocked(blloku, true);
        Route weather = d.sys.buildRoute(LocalDate.now(), stops, new RouteConditions(), new WeatherBasedRoute());
        assertFalse(weather.getOrderedStops().contains(blloku));
        assertTrue(weather.getSkippedStops().contains(blloku));
        assertTrue(new WeatherBasedRoute().getAverageSpeedKmh() < new NormalRoutineRoute().getAverageSpeedKmh());
    }

    @Test
    void specialPickupIsServedFirst() {
        RouteConditions c = new RouteConditions();
        ZoneLocation kombinat = d.sys.getZoneMap().findByName("Kombinat");
        c.getPriority().add(kombinat);
        Route r = d.sys.buildRoute(LocalDate.now(), stops, c, new SpecialPickupsRoute());
        assertEquals(kombinat, r.getOrderedStops().get(0));
    }

    @Test
    void centerChoosesTheStrategyFromTheConditions() {
        RouteOptimizationCenter center = RouteOptimizationCenter.getInstance();
        assertTrue(center.chooseStrategy(new RouteConditions()) instanceof NormalRoutineRoute);
        RouteConditions bad = new RouteConditions();
        bad.setSevereWeather(true);
        assertTrue(center.chooseStrategy(bad) instanceof WeatherBasedRoute);
        RouteConditions special = new RouteConditions();
        special.getPriority().add(stops.get(0));
        assertTrue(center.chooseStrategy(special) instanceof SpecialPickupsRoute);
    }

    @Test
    void findsTheNearestTruckAndTrucksDriveTheirRoute() {
        Vehicle nearest = RouteOptimizationCenter.getInstance().findNearestVehicle(new Coordinate(41.3190, 19.7840), d.sys.getVehicles());
        assertEquals("AB 456 CD", nearest.getPlateNumber());

        Route r = d.sys.buildRoute(LocalDate.now(), stops, new RouteConditions(), new NormalRoutineRoute());
        Vehicle truck = d.sys.getVehicles().get(0);
        d.sys.confirmRoute(r, truck, d.ardi);
        Coordinate start = truck.getCurrentLocation();
        d.sys.tick();
        assertNotEquals(start, truck.getCurrentLocation());
        assertTrue(d.ardi.getNotifications().get(0).getSubject().contains("New route"));
    }
}
