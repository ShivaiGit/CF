package com.example.cf.data

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class WeatherRepository(private val apiKey: String) {
    private val api: WeatherApiService

    init {
        Log.d("WeatherRepository", "Initializing API service")
        try {
            api = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApiService::class.java)
            Log.d("WeatherRepository", "API service initialized successfully")
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to initialize API service", e)
            throw e
        }
    }

    suspend fun getCurrentWeather(city: String): WeatherResponse {
        Log.d("WeatherRepository", "Fetching current weather for city: $city")
        try {
            val response = api.getCurrentWeather(city, apiKey)
            Log.d("WeatherRepository", "Successfully fetched current weather for: $city")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching current weather for: $city", e)
            throw e
        }
    }

    suspend fun getForecast(city: String): ForecastResponse {
        Log.d("WeatherRepository", "Fetching forecast for city: $city")
        try {
            val response = api.getForecast(city, apiKey)
            Log.d("WeatherRepository", "Successfully fetched forecast for: $city")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching forecast for: $city", e)
            throw e
        }
    }

    suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double): WeatherResponse {
        Log.d("WeatherRepository", "Fetching current weather for coords: $lat, $lon")
        try {
            val response = api.getCurrentWeatherByCoords(lat, lon, apiKey)
            Log.d("WeatherRepository", "Successfully fetched current weather for coords: $lat, $lon")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching current weather for coords: $lat, $lon", e)
            throw e
        }
    }

    suspend fun getForecastByCoords(lat: Double, lon: Double): ForecastResponse {
        Log.d("WeatherRepository", "Fetching forecast for coords: $lat, $lon")
        try {
            val response = api.getForecastByCoords(lat, lon, apiKey)
            Log.d("WeatherRepository", "Successfully fetched forecast for coords: $lat, $lon")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching forecast for coords: $lat, $lon", e)
            throw e
        }
    }
} 