package com.ratio.launcher.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class WeatherData(
    val tempC: Int,
    val tempF: Int,
    val condition: String,
    val humidity: Int,
    val windKmh: Int,
    val windMph: Int,
    val rainChance: Int,
    val feelsLikeC: Int,
    val feelsLikeF: Int,
    val tomorrowTempC: Int,
    val tomorrowTempF: Int,
    val tomorrowCondition: String,
    val city: String
)

object WeatherHelper {

    private const val PREFS_NAME = "ratio_weather"
    private const val KEY_CITY = "city"
    private const val KEY_CACHE = "cached_json"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_UNIT = "unit"
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getCity(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CITY, "") ?: ""
    }

    fun setCity(context: Context, city: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CITY, city).apply()
    }

    fun isMetric(context: Context): Boolean {
        return context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            .getString(KEY_UNIT, "metric") == "metric"
    }

    fun setUnit(context: Context, metric: Boolean) {
        context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY_UNIT, if (metric) "metric" else "imperial").apply()
    }

    fun getCachedWeather(context: Context): WeatherData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CACHE, null) ?: return null
        return try { parseWeatherJson(json, getCity(context)) } catch (_: Exception) { null }
    }

    fun fetchWeather(context: Context, city: String, callback: (WeatherData?) -> Unit) {
        if (city.isBlank()) {
            callback(null)
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getLong(KEY_TIMESTAMP, 0)
        val cachedCity = prefs.getString(KEY_CITY, "") ?: ""

        if (System.currentTimeMillis() - lastFetch < CACHE_DURATION_MS && cachedCity == city) {
            val cached = getCachedWeather(context)
            if (cached != null) {
                mainHandler.post { callback(cached) }
                return
            }
        }

        executor.execute {
            try {
                val url = URL("https://wttr.in/${city.trim()}?format=j1")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "RatioLauncher/1.0")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val weather = parseWeatherJson(response, city)

                prefs.edit()
                    .putString(KEY_CACHE, response)
                    .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                    .apply()

                mainHandler.post { callback(weather) }
            } catch (_: Exception) {
                mainHandler.post { callback(null) }
            }
        }
    }

    private fun parseWeatherJson(json: String, city: String): WeatherData {
        val root = JSONObject(json)
        val current = root.getJSONArray("current_condition").getJSONObject(0)
        val forecast = root.getJSONArray("weather")

        val todayForecast = forecast.getJSONObject(0)
        val tomorrowForecast = if (forecast.length() > 1) forecast.getJSONObject(1) else todayForecast

        val condition = current.getJSONArray("weatherDesc").getJSONObject(0).getString("value")

        val todayHourly = todayForecast.getJSONArray("hourly")
        var rainChance = 0
        for (i in 0 until todayHourly.length()) {
            val chance = todayHourly.getJSONObject(i).optString("chanceofrain", "0").toIntOrNull() ?: 0
            if (chance > rainChance) rainChance = chance
        }

        return WeatherData(
            tempC = current.getString("temp_C").toIntOrNull() ?: 0,
            tempF = current.getString("temp_F").toIntOrNull() ?: 0,
            condition = condition,
            humidity = current.getString("humidity").toIntOrNull() ?: 0,
            windKmh = current.getString("windspeedKmph").toIntOrNull() ?: 0,
            windMph = current.getString("windspeedMiles").toIntOrNull() ?: 0,
            rainChance = rainChance,
            feelsLikeC = current.getString("FeelsLikeC").toIntOrNull() ?: 0,
            feelsLikeF = current.getString("FeelsLikeF").toIntOrNull() ?: 0,
            tomorrowTempC = tomorrowForecast.getString("avgtempC").toIntOrNull() ?: 0,
            tomorrowTempF = tomorrowForecast.getString("avgtempF").toIntOrNull() ?: 0,
            tomorrowCondition = tomorrowForecast.getJSONArray("hourly")
                .getJSONObject(4).getJSONArray("weatherDesc")
                .getJSONObject(0).getString("value"),
            city = city
        )
    }

    fun formatTemp(data: WeatherData, metric: Boolean): String {
        return if (metric) "${data.tempC}°" else "${data.tempF}°"
    }

    fun formatWind(data: WeatherData, metric: Boolean): String {
        return if (metric) "${data.windKmh} km/h" else "${data.windMph} mph"
    }

    fun formatTomorrowTemp(data: WeatherData, metric: Boolean): String {
        return if (metric) "${data.tomorrowTempC}°" else "${data.tomorrowTempF}°"
    }
}
