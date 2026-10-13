package com.greenbin.wms.weather;

import java.time.LocalDate;
import java.util.List;

/** FR_07: the weather API the system talks to. A real one would call an online service. */
public interface IWeatherService {
    WeatherData getCurrentWeather(String location);
    WeatherData getDailyForecast(String location, LocalDate date);
    List<WeatherData> getWeeklyForecast(String location, LocalDate from);

    default boolean isSevereWeatherExpected(String location, LocalDate date) {
        return getDailyForecast(location, date).isSevere();
    }
}
