package com.greenbin.wms;

import com.greenbin.wms.planning.Statistics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FR_02 statistics and FR_30 prediction. */
class StatisticsTest {

    private Statistics stats(double... values) {
        Statistics s = new Statistics(2026);
        for (double v : values) s.addData("Blloku", v);
        return s;
    }

    @Test
    void meanMedianModeAndStandardDeviation() {
        Statistics s = stats(600, 620, 620, 640, 700);
        assertEquals(636, s.calculateMean("Blloku"), 0.001);
        assertEquals(620, s.calculateMedian("Blloku"), 0.001);
        assertEquals(620, s.calculateMode("Blloku"), 0.001);
        assertEquals(34.4093, s.calculateStandardDeviation("Blloku"), 0.001);
    }

    @Test
    void optimalPickupsIsWeeklyWasteDividedByTruckCapacity() {
        assertEquals(3, stats(650, 650).calculateOptimalPickups("Blloku"));     // 4550 kg / 2000 = 2.3 -> 3
        assertEquals(1, stats(10).calculateOptimalPickups("Blloku"));           // at least one pickup
    }

    @Test
    void predictionFollowsTheTrend() {
        Statistics s = stats(100, 110, 120, 130, 140);
        assertEquals(10, s.getTrend("Blloku"), 0.001);
        List<Double> next = s.predictNextDays("Blloku", 2);
        assertEquals(150, next.get(0), 0.001);
        assertEquals(160, next.get(1), 0.001);
    }

    @Test
    void negativeAmountsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> stats().addData("Blloku", -5));
    }
}
