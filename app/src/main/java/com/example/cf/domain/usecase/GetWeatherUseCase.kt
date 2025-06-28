package com.example.cf.domain.usecase

import com.example.cf.domain.model.WeatherResponse
import com.example.cf.domain.repository.WeatherRepository
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(city: String, units: String): WeatherResponse {
        return repository.getCurrentWeather(city, units)
    }
} 