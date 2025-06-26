package com.example.cf.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository(private val apiKey: String) {
    private val api: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)

    suspend fun getCurrentWeather(city: String) =
        api.getCurrentWeather(city, apiKey)

    suspend fun getForecast(city: String) =
        api.getForecast(city, apiKey)
} 