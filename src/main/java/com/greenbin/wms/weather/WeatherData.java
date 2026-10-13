package com.greenbin.wms.weather;

import java.time.LocalDate;

/** The weather for one day in one place. */
public class WeatherData {
    public enum Condition {
        SUNNY("Sunny", false), CLOUDY("Cloudy", false), RAIN("Rain", false),
        STORM("Storm", true), HEAVY_SNOW("Heavy snow", true), HEAT_WAVE("Heat wave", true);

        private final String label;
        private final boolean severe;
        Condition(String label, boolean severe) { this.label = label; this.severe = severe; }
        public String getLabel() { return label; }
        public boolean isSevere() { return severe; }
        @Override public String toString() { return label; }
    }

    private final LocalDate date;
    private final String location;
    private final Condition condition;
    private final double temperature;
    private final double humidity;
    private final double windSpeed;

    public WeatherData(LocalDate date, String location, Condition condition,
                       double temperature, double humidity, double windSpeed) {
        this.date = date;
        this.location = location;
        this.condition = condition;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
    }

    /** Extreme conditions: storm, heavy snow, heat wave, or very strong wind. */
    public boolean isSevere() { return condition.isSevere() || windSpeed >= 70; }

    public LocalDate getDate() { return date; }
    public String getLocation() { return location; }
    public Condition getCondition() { return condition; }
    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }

    @Override
    public String toString() {
        return date + " " + condition + " " + Math.round(temperature) + "°C, wind " + Math.round(windSpeed) + " km/h";
    }
}
