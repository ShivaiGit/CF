package com.example.cf.domain.usecase

import com.example.cf.domain.model.ForecastResponse
import com.example.cf.domain.repository.WeatherRepository
import javax.inject.Inject

class GetForecastUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(city: String, units: String): ForecastResponse {
        return repository.getForecast(city, units)
    }
} 