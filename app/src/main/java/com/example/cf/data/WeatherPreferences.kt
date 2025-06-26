package com.example.cf.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_preferences")

class WeatherPreferences(private val context: Context) {
    
    private val LAST_CITY = stringPreferencesKey("last_city")
    private val DARK_THEME = stringPreferencesKey("dark_theme")
    private val UNIT_CELSIUS = stringPreferencesKey("unit_celsius")
    private val CACHE_WEATHER = stringPreferencesKey("cache_weather")
    private val CACHE_FORECAST = stringPreferencesKey("cache_forecast")
    private val CACHE_TIMESTAMP = stringPreferencesKey("cache_timestamp")
    private val HISTORY_CITIES = stringPreferencesKey("history_cities")
    private val gson = Gson()

    init {
        Log.d("WeatherPreferences", "Initializing preferences")
    }

    val lastCity: Flow<String> = context.dataStore.data
        .catch { exception ->
            Log.e("WeatherPreferences", "Error reading preferences", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                val city = preferences[LAST_CITY] ?: ""
                Log.d("WeatherPreferences", "Read last city: $city")
                city
            } catch (e: Exception) {
                Log.e("WeatherPreferences", "Error mapping preferences", e)
                ""
            }
        }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            Log.e("WeatherPreferences", "Error reading preferences", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[DARK_THEME]?.toBooleanStrictOrNull() ?: false
        }

    val isCelsius: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            Log.e("WeatherPreferences", "Error reading preferences", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[UNIT_CELSIUS]?.toBooleanStrictOrNull() ?: true
        }

    // --- Кэш погоды ---
    val cachedWeather: Flow<String?> = context.dataStore.data
        .catch { exception ->
            Log.e("WeatherPreferences", "Error reading cached weather", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[CACHE_WEATHER]
        }

    suspend fun saveCachedWeather(json: String) {
        try {
            Log.d("WeatherPreferences", "Saving cached weather")
            context.dataStore.edit { preferences ->
                preferences[CACHE_WEATHER] = json
            }
        } catch (e: Exception) {
            Log.e("WeatherPreferences", "Error saving cached weather", e)
        }
    }

    // --- Кэш прогноза ---
    val cachedForecast: Flow<String?> = context.dataStore.data
        .catch { exception ->
            Log.e("WeatherPreferences", "Error reading cached forecast", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[CACHE_FORECAST]
        }

    suspend fun saveCachedForecast(json: String) {
        try {
            Log.d("WeatherPreferences", "Saving cached forecast")
            context.dataStore.edit { preferences ->
                preferences[CACHE_FORECAST] = json
            }
        } catch (e: Exception) {
            Log.e("WeatherPreferences", "Error saving cached forecast", e)
        }
    }

    val cachedTimestamp: Flow<Long?> = context.dataStore.data
        .catch { exception ->
            Log.e("WeatherPreferences", "Error reading cached timestamp", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[CACHE_TIMESTAMP]?.toLongOrNull()
        }

    suspend fun saveCachedTimestamp(timestamp: Long) {
        try {
            Log.d("WeatherPreferences", "Saving cached timestamp: $timestamp")
            context.dataStore.edit { preferences ->
                preferences[CACHE_TIMESTAMP] = timestamp.toString()
            }
        } catch (e: Exception) {
            Log.e("WeatherPreferences", "Error saving cached timestamp", e)
        }
    }

    suspend fun saveCity(city: String) {
        try {
            Log.d("WeatherPreferences", "Saving city: $city")
            context.dataStore.edit { preferences ->
                preferences[LAST_CITY] = city
            }
            Log.d("WeatherPreferences", "Successfully saved city: $city")
        } catch (e: Exception) {
            Log.e("WeatherPreferences", "Error saving city: $city", e)
            throw e
        }
    }

    suspend fun saveDarkTheme(isDark: Boolean) {
        try {
            Log.d("WeatherPreferences", "Saving dark theme: $isDark")
            context.dataStore.edit { preferences ->
                preferences[DARK_THEME] = isDark.toString()
            }
            Log.d("WeatherPreferences", "Successfully saved dark theme: $isDark")
        } catch (e: Exception) {
            Log.e("WeatherPreferences", "Error saving dark theme: $isDark", e)
            throw e
        }
    }

    suspend fun saveUnit(isCelsius: Boolean) {
        try {
            Log.d("WeatherPreferences", "Saving unit: $isCelsius")
            context.dataStore.edit { preferences ->
                preferences[UNIT_CELSIUS] = isCelsius.toString()
            }
            Log.d("WeatherPreferences", "Successfully saved unit: $isCelsius")
        } catch (e: Exception) {
            Log.e("WeatherPreferences", "Error saving unit: $isCelsius", e)
            throw e
        }
    }

    val historyCities: Flow<List<String>> = context.dataStore.data
        .catch { exception ->
            Log.e("WeatherPreferences", "Error reading history cities", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[HISTORY_CITIES]?.let {
                try {
                    gson.fromJson(it, Array<String>::class.java).toList()
                } catch (e: Exception) {
                    Log.e("WeatherPreferences", "Error parsing history cities", e)
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun saveHistoryCities(cities: List<String>) {
        try {
            val json = gson.toJson(cities)
            context.dataStore.edit { preferences ->
                preferences[HISTORY_CITIES] = json
            }
        } catch (e: Exception) {
            Log.e("WeatherPreferences", "Error saving history cities", e)
        }
    }

    suspend fun addCityToHistory(city: String) {
        val current = historyCities.first().toMutableList()
        current.removeAll { it.equals(city, ignoreCase = true) }
        current.add(0, city)
        val trimmed = current.take(5)
        saveHistoryCities(trimmed)
    }

    suspend fun clearHistoryCities() {
        saveHistoryCities(emptyList())
    }
} 