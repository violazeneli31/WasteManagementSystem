package com.greenbin.wms.weather;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A stand-in for the real weather API, so the project runs offline.
 * Every week has the same pattern (a storm on Wednesdays), and the manager
 * can change any day from the screen to try other situations.
 */
public class SimulatedWeatherService implements IWeatherService {
    private static final WeatherData.Condition[] WEEK = {
            WeatherData.Condition.SUNNY,   // Monday
            WeatherData.Condition.CLOUDY,  // Tuesday
            WeatherData.Condition.STORM,   // Wednesday
            WeatherData.Condition.RAIN,    // Thursday
            WeatherData.Condition.SUNNY,   // Friday
            WeatherData.Condition.SUNNY,   // Saturday
            WeatherData.Condition.CLOUDY   // Sunday
    };

    private final Map<LocalDate, WeatherData.Condition> changedDays = new HashMap<>();

    @Override
    public WeatherData getCurrentWeather(String location) {
        return getDailyForecast(location, LocalDate.now());
    }

    @Override
    public WeatherData getDailyForecast(String location, LocalDate date) {
        WeatherData.Condition c = changedDays.getOrDefault(date, WEEK[date.getDayOfWeek().getValue() - 1]);
        double temp;
        double wind;
        switch (c) {
            case STORM:      temp = 12; wind = 85; break;
            case HEAVY_SNOW: temp = -3; wind = 40; break;
            case HEAT_WAVE:  temp = 41; wind = 10; break;
            case RAIN:       temp = 14; wind = 25; break;
            case CLOUDY:     temp = 18; wind = 15; break;
            default:         temp = 24; wind = 8;
        }
        return new WeatherData(date, location, c, temp, c == WeatherData.Condition.SUNNY ? 45 : 80, wind);
    }

    @Override
    public List<WeatherData> getWeeklyForecast(String location, LocalDate from) {
        List<WeatherData> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) week.add(getDailyForecast(location, from.plusDays(i)));
        return week;
    }

    /** Used by the screen to simulate a different forecast for a day. */
    public void setCondition(LocalDate date, WeatherData.Condition condition) {
        changedDays.put(date, condition);
    }
}
