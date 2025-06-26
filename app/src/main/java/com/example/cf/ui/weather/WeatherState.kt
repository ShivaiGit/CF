package com.example.cf.ui.weather

import com.example.cf.data.WeatherResponse
import com.example.cf.data.ForecastResponse

data class WeatherState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val weather: WeatherResponse? = null,
    val forecast: ForecastResponse? = null,
    val city: String = "",
    val isDarkTheme: Boolean = false
) 