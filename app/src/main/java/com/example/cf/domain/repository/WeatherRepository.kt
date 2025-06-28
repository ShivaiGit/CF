package com.example.cf.domain.repository

import com.example.cf.domain.model.WeatherResponse
import com.example.cf.domain.model.ForecastResponse

interface WeatherRepository {
    suspend fun getCurrentWeather(city: String, units: String): WeatherResponse
    suspend fun getForecast(city: String, units: String): ForecastResponse
    suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double, units: String): WeatherResponse
    suspend fun getForecastByCoords(lat: Double, lon: Double, units: String): ForecastResponse
} 