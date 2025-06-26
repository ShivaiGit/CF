package com.example.cf.ui.weather

import com.example.cf.data.WeatherResponse

data class WeatherState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val weather: WeatherResponse? = null,
    val city: String = ""
) 