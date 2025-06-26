package com.example.cf.ui.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cf.data.WeatherRepository
import com.example.cf.data.WeatherPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.example.cf.data.WeatherResponse
import com.example.cf.data.ForecastResponse

sealed class LocationEvent {
    object RequestLocation : LocationEvent()
    data class LocationResult(val lat: Double, val lon: Double) : LocationEvent()
    data class Error(val message: String) : LocationEvent()
}

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val preferences: WeatherPreferences
) : ViewModel() {

    private val gson = Gson()
    private val _state = MutableStateFlow(WeatherState())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    private val _locationEvents = Channel<LocationEvent>(Channel.BUFFERED)
    val locationEvents: ReceiveChannel<LocationEvent> = _locationEvents

    private val _isCacheShown = MutableStateFlow(false)
    val isCacheShown: StateFlow<Boolean> = _isCacheShown.asStateFlow()

    private val _cacheTimestamp = MutableStateFlow<Long?>(null)
    val cacheTimestamp: StateFlow<Long?> = _cacheTimestamp.asStateFlow()

    init {
        Log.d("WeatherViewModel", "Initializing ViewModel")
        loadTheme()
        loadSavedCity()
    }

    private fun loadSavedCity() {
        viewModelScope.launch {
            try {
                val lastCity = preferences.lastCity.first()
                Log.d("WeatherViewModel", "Loaded city: $lastCity")
                if (lastCity.isNotBlank()) {
                    _state.update { it.copy(city = lastCity) }
                    fetchWeather(isAutoLoad = true)
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error loading saved city", e)
                clearSavedCity()
            }
        }
    }

    fun onCityChange(newCity: String) {
        _state.update { it.copy(city = newCity, error = null) }
    }

    private fun clearSavedCity() {
        viewModelScope.launch {
            try {
                Log.d("WeatherViewModel", "Clearing saved city")
                preferences.saveCity("")
                _state.update { it.copy(city = "", weather = null, forecast = null) }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error clearing saved city", e)
            }
        }
    }

    fun fetchWeather(isAutoLoad: Boolean = false) {
        val cityToFetch = _state.value.city
        val units = if (_state.value.isCelsius) "metric" else "imperial"
        if (cityToFetch.isBlank()) {
            Log.d("WeatherViewModel", "Attempted to fetch weather with blank city")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("WeatherViewModel", "Starting weather fetch for city: $cityToFetch")
                _state.update { it.copy(isLoading = true, error = null) }

                // Параллельно загружаем текущую погоду и прогноз
                val currentDeferred = async { repository.getCurrentWeather(cityToFetch, units) }
                val forecastDeferred = async { repository.getForecast(cityToFetch, units) }

                val current = currentDeferred.await()
                val forecast = forecastDeferred.await()

                Log.d("WeatherViewModel", "Successfully fetched weather for: $cityToFetch")
                
                // Сохраняем город ТОЛЬКО после успешного получения данных
                preferences.saveCity(cityToFetch)

                // --- Кэшируем ---
                try {
                    preferences.saveCachedWeather(gson.toJson(current))
                    preferences.saveCachedForecast(gson.toJson(forecast))
                    preferences.saveCachedTimestamp(System.currentTimeMillis())
                    _cacheTimestamp.value = System.currentTimeMillis()
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Error caching weather/forecast", e)
                }

                _state.update { 
                    it.copy(
                        isLoading = false,
                        weather = current,
                        forecast = forecast,
                        error = null
                    )
                }
                _isCacheShown.value = false
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather for $cityToFetch", e)
                // Если это автозагрузка и возникла ошибка - очищаем сохранённый город
                if (isAutoLoad) {
                    Log.d("WeatherViewModel", "Auto-load failed, clearing saved city")
                    clearSavedCity()
                }
                // --- Пробуем загрузить кэш ---
                var cacheUsed = false
                try {
                    val cachedWeatherJson = preferences.cachedWeather.first()
                    val cachedForecastJson = preferences.cachedForecast.first()
                    val cachedTimestampValue = preferences.cachedTimestamp.first()
                    if (!cachedWeatherJson.isNullOrBlank() && !cachedForecastJson.isNullOrBlank()) {
                        val cachedWeather = gson.fromJson(cachedWeatherJson, WeatherResponse::class.java)
                        val cachedForecast = gson.fromJson(cachedForecastJson, ForecastResponse::class.java)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                weather = cachedWeather,
                                forecast = cachedForecast,
                                error = errorMessage(e)
                            )
                        }
                        _isCacheShown.value = true
                        _cacheTimestamp.value = cachedTimestampValue
                        cacheUsed = true
                    }
                } catch (ex: Exception) {
                    Log.e("WeatherViewModel", "Error loading cache", ex)
                }
                if (!cacheUsed) {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = errorMessage(e)
                        )
                    }
                    _isCacheShown.value = false
                }
            }
        }
    }

    private fun errorMessage(e: Exception): String {
        val message = when (e) {
            is IOException -> "Нет подключения к интернету. Проверьте сеть."
            is HttpException -> when (e.code()) {
                404 -> "Город не найден. Проверьте название."
                401 -> "Ошибка авторизации. Проверьте API ключ."
                else -> "Ошибка сервера: ${e.code()}"
            }
            else -> e.message ?: "Неизвестная ошибка. Попробуйте позже."
        }
        Log.d("WeatherViewModel", "Error message: $message")
        return message
    }

    fun toggleTheme() {
        val newTheme = !_state.value.isDarkTheme
        _state.update { it.copy(isDarkTheme = newTheme) }
        viewModelScope.launch {
            try {
                preferences.saveDarkTheme(newTheme)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error saving theme", e)
            }
        }
    }

    private fun loadTheme() {
        viewModelScope.launch {
            try {
                val isDark = preferences.isDarkTheme.first()
                _state.update { it.copy(isDarkTheme = isDark) }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error loading theme", e)
            }
        }
    }

    fun onMyLocationClick() {
        Log.d("WeatherViewModel", "onMyLocationClick called")
        viewModelScope.launch {
            _locationEvents.send(LocationEvent.RequestLocation)
        }
    }

    fun onLocationReceived(lat: Double, lon: Double) {
        val units = if (_state.value.isCelsius) "metric" else "imperial"
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                val currentDeferred = async { repository.getCurrentWeatherByCoords(lat, lon, units) }
                val forecastDeferred = async { repository.getForecastByCoords(lat, lon, units) }
                val current = currentDeferred.await()
                val forecast = forecastDeferred.await()
                _state.update {
                    it.copy(
                        isLoading = false,
                        weather = current,
                        forecast = forecast,
                        error = null,
                        city = current.name
                    )
                }
                preferences.saveCity(current.name)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather by location", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage(e)
                    )
                }
            }
        }
    }

    fun onLocationError(message: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = message, isLoading = false) }
        }
    }

    fun onUnitChange(isCelsius: Boolean) {
        _state.update { it.copy(isCelsius = isCelsius) }
        viewModelScope.launch {
            try {
                preferences.saveUnit(isCelsius)
                // Перезапрашиваем погоду с новыми units
                fetchWeather()
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error saving unit", e)
            }
        }
    }
} 