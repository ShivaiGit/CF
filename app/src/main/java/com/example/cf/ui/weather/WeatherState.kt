package com.example.cf.ui.weather

import com.example.cf.data.WeatherResponse
import com.example.cf.data.ForecastResponse
import com.example.cf.core.Result

data class WeatherState(
    val weatherResult: Result<WeatherResponse> = Result.Loading,
    val forecastResult: Result<ForecastResponse> = Result.Loading,
    val city: String = "",
    val isDarkTheme: Boolean = false,
    val isCelsius: Boolean = true
) {
    val isLoading: Boolean
        get() = weatherResult.isLoading() || forecastResult.isLoading()
    
    val error: String?
        get() = when {
            weatherResult.isError() -> weatherResult.exceptionOrNull()?.message
            forecastResult.isError() -> forecastResult.exceptionOrNull()?.message
            else -> null
        }
    
    val weather: WeatherResponse?
        get() = weatherResult.getOrNull()
    
    val forecast: ForecastResponse?
        get() = forecastResult.getOrNull()
} 