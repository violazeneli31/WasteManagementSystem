package com.greenbin.wms;

import com.greenbin.wms.bins.DisposalRecord;
import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.model.BinStatus;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.notification.RewardNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FR_08 barcode, FR_09 capacity sensors, FR_12 reward points, FR_14 error handling. */
class SmartBinTest {
    private TestData d;
    private SmartBin plasticBin;

    @BeforeEach
    void setUp() {
        d = new TestData();
        plasticBin = new SmartBin("Blloku", new Coordinate(41.3200, 19.8160), 100, 100000001L, WasteType.PLASTIC);
        d.sys.getContainers().add(plasticBin);
    }

    @Test
    void correctWasteGivesFullPointsAndARewardNotification() {
        DisposalRecord r = d.sys.disposeWaste(d.ana, plasticBin, WasteType.PLASTIC, 4);
        assertTrue(r.isCorrect());
        assertEquals(12, r.getPoints());                     // 4 kg x 3 points
        assertEquals(12, d.ana.getPoints());
        assertEquals(1, plasticBin.getBarcode().getScanCount());
        assertTrue(d.ana.getNotifications().get(0) instanceof RewardNotification);
    }

    @Test
    void wrongWasteGivesPartOfThePointsAndHazardousNone() {
        DisposalRecord paper = d.sys.disposeWaste(d.ana, plasticBin, WasteType.PAPER, 10);   // 20 points * 25%
        assertFalse(paper.isCorrect());
        assertEquals(5, paper.getPoints());
        assertTrue(plasticBin.getDisplayMessage().startsWith("Wrong bin"));
        DisposalRecord battery = d.sys.disposeWaste(d.ana, plasticBin, WasteType.HAZARDOUS, 1);
        assertEquals(0, battery.getPoints());
    }

    @Test
    void frequentErrorsAlertTheManager() {
        for (int i = 0; i < 3; i++) d.sys.disposeWaste(d.ben, plasticBin, WasteType.PAPER, 1);
        assertEquals(3, plasticBin.countRecentErrors(7));
        assertTrue(d.manager.getNotifications().get(0).getSubject().contains("Frequent errors"));
    }

    @Test
    void fullBinAlertsTheManagerAndPlansACollection() {
        plasticBin.updateFillLevel(91);
        assertEquals(BinStatus.FULL, plasticBin.getStatus());
        assertNotNull(d.sys.checkContainer(plasticBin));
        assertNull(d.sys.checkContainer(plasticBin));          // only one alert
        Schedule s = d.sys.getSchedules().get(0);
        assertTrue(s.isSpecial());
        assertFalse(d.sys.getWeather().isSevereWeatherExpected("Tirana", s.getDate()));
        assertTrue(d.manager.getNotifications().get(0).getSubject().contains("Bin almost full"));
    }

    @Test
    void brokenSensorCreatesAMaintenanceRequest() {
        plasticBin.getSensor().setWorking(false);
        d.sys.readAllSensors();
        assertEquals(1, d.sys.getMaintenanceRequests().size());
        assertEquals(-1, plasticBin.readSensor(), 0.001);
    }

    @Test
    void emptyingSendsWrongWasteToTheSeparateCompartment() {
        d.sys.disposeWaste(d.ana, plasticBin, WasteType.PLASTIC, 5);
        d.sys.disposeWaste(d.ana, plasticBin, WasteType.PAPER, 2);
        List<Waste> load = d.sys.emptyBin(plasticBin);
        assertEquals(2, load.size());
        assertEquals(0, plasticBin.getFillPercent(), 0.001);
        assertEquals(1, d.sys.getFacility().getFlaggedCompartment().size());
    }

    @Test
    void blockedUsersCannotThrowWaste() {
        d.ana.setBlocked(true);
        assertThrows(IllegalStateException.class, () -> d.sys.disposeWaste(d.ana, plasticBin, WasteType.PLASTIC, 1));
    }
}
