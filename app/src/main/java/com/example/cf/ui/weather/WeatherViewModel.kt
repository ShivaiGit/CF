package com.example.cf.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cf.data.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherState())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    fun onCityChange(newCity: String) {
        _state.update { it.copy(city = newCity) }
    }

    fun fetchWeather() {
        if (_state.value.city.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Параллельно загружаем текущую погоду и прогноз
                val currentDeferred = async { repository.getCurrentWeather(_state.value.city) }
                val forecastDeferred = async { repository.getForecast(_state.value.city) }

                val current = currentDeferred.await()
                val forecast = forecastDeferred.await()

                _state.update { 
                    it.copy(
                        isLoading = false,
                        weather = current,
                        forecast = forecast
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
} 