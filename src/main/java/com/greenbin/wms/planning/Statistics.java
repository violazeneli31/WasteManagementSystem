package com.greenbin.wms.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Waste statistics per area (kg collected per day).
 * FR_02 uses them to calculate the optimal number of pickups per area,
 * FR_30 uses them to predict the next days.
 */
public class Statistics {
    /** One truck trip can carry about this much. */
    public static final double TRUCK_CAPACITY_KG = 2000;

    private final int year;
    private final Map<String, List<Double>> data = new LinkedHashMap<>();

    public Statistics(int year) { this.year = year; }

    public void addData(String area, double kg) {
        if (kg < 0) throw new IllegalArgumentException("Waste amount can't be negative");
        data.computeIfAbsent(area, a -> new ArrayList<>()).add(kg);
    }

    public void removeData(String area, double kg) {
        List<Double> values = data.get(area);
        if (values != null) values.remove(kg);
    }

    public List<String> getAreas() { return new ArrayList<>(data.keySet()); }
    public List<Double> getData(String area) { return data.getOrDefault(area, new ArrayList<>()); }
    public int getYear() { return year; }

    public double calculateMean(String area) {
        List<Double> v = getData(area);
        if (v.isEmpty()) return 0;
        double sum = 0;
        for (double d : v) sum += d;
        return sum / v.size();
    }

    public double calculateMedian(String area) {
        List<Double> v = new ArrayList<>(getData(area));
        if (v.isEmpty()) return 0;
        Collections.sort(v);
        int mid = v.size() / 2;
        return v.size() % 2 == 1 ? v.get(mid) : (v.get(mid - 1) + v.get(mid)) / 2;
    }

    /** The most common value, rounded to tens of kg (exact values rarely repeat). */
    public double calculateMode(String area) {
        Map<Long, Integer> counts = new HashMap<>();
        long best = 0;
        int bestCount = 0;
        for (double d : getData(area)) {
            long rounded = Math.round(d / 10) * 10;
            int c = counts.merge(rounded, 1, Integer::sum);
            if (c > bestCount || (c == bestCount && rounded > best)) { best = rounded; bestCount = c; }
        }
        return best;
    }

    public double calculateStandardDeviation(String area) {
        List<Double> v = getData(area);
        if (v.size() < 2) return 0;
        double mean = calculateMean(area);
        double sum = 0;
        for (double d : v) sum += (d - mean) * (d - mean);
        return Math.sqrt(sum / v.size());
    }

    /** FR_02: weekly pickups needed = average week of waste / truck capacity (at least 1). */
    public int calculateOptimalPickups(String area) {
        double weekly = calculateMean(area) * 7;
        return Math.max(1, (int) Math.ceil(weekly / TRUCK_CAPACITY_KG));
    }

    public Map<String, Integer> calculateOptimalPickupsPerArea() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String area : data.keySet()) result.put(area, calculateOptimalPickups(area));
        return result;
    }

    /** How many kg per day the waste grows (+) or falls (-): the slope of a straight line through the data. */
    public double getTrend(String area) {
        List<Double> v = getData(area);
        int n = v.size();
        if (n < 2) return 0;
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int x = 0; x < n; x++) {
            sumX += x; sumY += v.get(x); sumXY += x * v.get(x); sumXX += x * x;
        }
        return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    }

    /** FR_30: predicted kg for the next days, following the trend line. */
    public List<Double> predictNextDays(String area, int days) {
        List<Double> v = getData(area);
        List<Double> prediction = new ArrayList<>();
        if (v.isEmpty()) return prediction;
        double slope = getTrend(area);
        double mean = calculateMean(area);
        double middle = (v.size() - 1) / 2.0;
        for (int i = 1; i <= days; i++) {
            double x = v.size() - 1 + i;
            prediction.add(Math.max(0, mean + slope * (x - middle)));
        }
        return prediction;
    }

    public double predictNextWeekTotal(String area) {
        double total = 0;
        for (double d : predictNextDays(area, 7)) total += d;
        return total;
    }
}
