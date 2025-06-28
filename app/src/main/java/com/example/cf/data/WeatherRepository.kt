package com.example.cf.data

import android.util.Log
import java.io.IOException

class WeatherRepository(
    private val apiKey: String,
    private val api: WeatherApiService
) {
    init {
        Log.d("WeatherRepository", "Initializing API service")
    }

    suspend fun getCurrentWeather(city: String, units: String): WeatherResponse {
        Log.d("WeatherRepository", "Fetching current weather for city: $city, units: $units")
        try {
            val response = api.getCurrentWeather(city, apiKey, units)
            Log.d("WeatherRepository", "Successfully fetched current weather for: $city, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching current weather for: $city, units: $units", e)
            throw e
        }
    }

    suspend fun getForecast(city: String, units: String): ForecastResponse {
        Log.d("WeatherRepository", "Fetching forecast for city: $city, units: $units")
        try {
            val response = api.getForecast(city, apiKey, units)
            Log.d("WeatherRepository", "Successfully fetched forecast for: $city, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching forecast for: $city, units: $units", e)
            throw e
        }
    }

    suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double, units: String): WeatherResponse {
        Log.d("WeatherRepository", "Fetching current weather for coords: $lat, $lon, units: $units")
        try {
            val response = api.getCurrentWeatherByCoords(lat, lon, apiKey, units)
            Log.d("WeatherRepository", "Successfully fetched current weather for coords: $lat, $lon, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching current weather for coords: $lat, $lon, units: $units", e)
            throw e
        }
    }

    suspend fun getForecastByCoords(lat: Double, lon: Double, units: String): ForecastResponse {
        Log.d("WeatherRepository", "Fetching forecast for coords: $lat, $lon, units: $units")
        try {
            val response = api.getForecastByCoords(lat, lon, apiKey, units)
            Log.d("WeatherRepository", "Successfully fetched forecast for coords: $lat, $lon, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching forecast for coords: $lat, $lon, units: $units", e)
            throw e
        }
    }
} 