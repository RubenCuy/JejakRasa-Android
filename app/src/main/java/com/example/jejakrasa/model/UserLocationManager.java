package com.example.jejakrasa.model;

public class UserLocationManager {
    private static String cityFilter = "";
    private static String displayLabel = "Semua Kota (Ketuk untuk atur)";
    private static String originForMaps = "";

    public static void setGpsLocation(double lat, double lng, String detectedCityFilter, String label) {
        originForMaps = lat + "," + lng;
        cityFilter = detectedCityFilter != null ? detectedCityFilter : "";
        displayLabel = label;
    }

    public static void setManualCity(String selectedCityFilter, String label) {
        originForMaps = selectedCityFilter != null ? selectedCityFilter : "";
        cityFilter = selectedCityFilter != null ? selectedCityFilter : "";
        displayLabel = label;
    }

    public static void clearLocation() {
        cityFilter = "";
        originForMaps = "";
        displayLabel = "Semua Kota (Ketuk untuk atur)";
    }

    public static String getCityFilter() {
        return cityFilter;
    }

    public static String getDisplayLabel() {
        return displayLabel;
    }

    public static String getOriginForMaps() {
        return originForMaps;
    }

    public static boolean hasOrigin() {
        return originForMaps != null && !originForMaps.isEmpty();
    }
}