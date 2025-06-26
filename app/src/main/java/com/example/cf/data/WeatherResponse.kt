package com.example.cf.data

data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind,
    val sys: Sys,
    val clouds: Clouds,
    val name: String
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val humidity: Int,
    val pressure: Int
)

data class Wind(
    val speed: Double
)

data class Sys(
    val sunrise: Long,
    val sunset: Long
)

data class Clouds(
    val all: Int
) 