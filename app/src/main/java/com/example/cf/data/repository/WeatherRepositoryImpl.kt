package com.example.cf.data.repository

import android.util.Log
import com.example.cf.data.remote.WeatherApiService
import com.example.cf.domain.model.WeatherResponse
import com.example.cf.domain.model.ForecastResponse
import com.example.cf.domain.repository.WeatherRepository
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val apiKey: String,
    private val api: WeatherApiService
) : WeatherRepository {

    init {
        Log.d("WeatherRepositoryImpl", "Initializing API service")
    }

    override suspend fun getCurrentWeather(city: String, units: String): WeatherResponse {
        Log.d("WeatherRepositoryImpl", "Fetching current weather for city: $city, units: $units")
        try {
            val response = api.getCurrentWeather(city, apiKey, units)
            Log.d("WeatherRepositoryImpl", "Successfully fetched current weather for: $city, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching current weather for: $city, units: $units", e)
            throw e
        }
    }

    override suspend fun getForecast(city: String, units: String): ForecastResponse {
        Log.d("WeatherRepositoryImpl", "Fetching forecast for city: $city, units: $units")
        try {
            val response = api.getForecast(city, apiKey, units)
            Log.d("WeatherRepositoryImpl", "Successfully fetched forecast for: $city, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching forecast for: $city, units: $units", e)
            throw e
        }
    }

    override suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double, units: String): WeatherResponse {
        Log.d("WeatherRepositoryImpl", "Fetching current weather for coords: $lat, $lon, units: $units")
        try {
            val response = api.getCurrentWeatherByCoords(lat, lon, apiKey, units)
            Log.d("WeatherRepositoryImpl", "Successfully fetched current weather for coords: $lat, $lon, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching current weather for coords: $lat, $lon, units: $units", e)
            throw e
        }
    }

    override suspend fun getForecastByCoords(lat: Double, lon: Double, units: String): ForecastResponse {
        Log.d("WeatherRepositoryImpl", "Fetching forecast for coords: $lat, $lon, units: $units")
        try {
            val response = api.getForecastByCoords(lat, lon, apiKey, units)
            Log.d("WeatherRepositoryImpl", "Successfully fetched forecast for coords: $lat, $lon, units: $units")
            return response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching forecast for coords: $lat, $lon, units: $units", e)
            throw e
        }
    }
} 