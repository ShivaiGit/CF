package com.example.cf.ui.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cf.domain.repository.WeatherRepository
import com.example.cf.data.WeatherPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.example.cf.domain.model.WeatherResponse
import com.example.cf.domain.model.ForecastResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.cf.core.Result
import com.example.cf.core.ErrorHandler

sealed class LocationEvent {
    object RequestLocation : LocationEvent()
    data class LocationResult(val lat: Double, val lon: Double) : LocationEvent()
    data class Error(val message: String) : LocationEvent()
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
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

    private val _historyCities = MutableStateFlow<List<String>>(emptyList())
    val historyCities: StateFlow<List<String>> = _historyCities.asStateFlow()

    init {
        Log.d("WeatherViewModel", "Initializing ViewModel")
        try {
            loadSavedCity()
            viewModelScope.launch {
                try {
                    preferences.historyCities.collect {
                        Log.d("WeatherViewModel", "History cities updated: $it")
                        _historyCities.value = it
                    }
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Error collecting history cities", e)
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error in init", e)
        }
    }

    private fun loadSavedCity() {
        viewModelScope.launch {
            try {
                val lastCity = preferences.lastCity.first()
                Log.d("WeatherViewModel", "Loaded city: '$lastCity'")
                if (lastCity.isNotBlank()) {
                    _state.update { it.copy(city = lastCity) }
                    // Автоматически загружаем погоду для последнего города
                    fetchWeather(isAutoLoad = true)
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error loading saved city", e)
                clearSavedCity()
            }
        }
    }

    fun onCityChange(newCity: String) {
        Log.d("WeatherViewModel", "City changed to: '$newCity'")
        _state.update { it.copy(city = newCity) }
    }

    private fun clearSavedCity() {
        viewModelScope.launch {
            try {
                Log.d("WeatherViewModel", "Clearing saved city")
                preferences.saveCity("")
                _state.update { it.copy(city = "", weatherResult = Result.Loading, forecastResult = Result.Loading) }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error clearing saved city", e)
            }
        }
    }

    fun fetchWeather(isAutoLoad: Boolean = false) {
        val cityToFetch = _state.value.city
        val units = if (_state.value.isCelsius) "metric" else "imperial"
        Log.d("WeatherViewModel", "fetchWeather called for city: '$cityToFetch', isAutoLoad: $isAutoLoad")
        
        if (cityToFetch.isBlank()) {
            Log.d("WeatherViewModel", "Attempted to fetch weather with blank city")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("WeatherViewModel", "Starting weather fetch for city: $cityToFetch")
                _state.update { 
                    it.copy(
                        weatherResult = Result.Loading,
                        forecastResult = Result.Loading
                    ) 
                }

                // Параллельно загружаем текущую погоду и прогноз
                val currentDeferred = async { repository.getCurrentWeather(cityToFetch, units) }
                val forecastDeferred = async { repository.getForecast(cityToFetch, units) }

                val current = currentDeferred.await()
                val forecast = forecastDeferred.await()

                Log.d("WeatherViewModel", "Successfully fetched weather for: $cityToFetch")
                
                // Сохраняем город ТОЛЬКО после успешного получения данных
                preferences.saveCity(cityToFetch)
                // Добавляем в историю
                preferences.addCityToHistory(cityToFetch)

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
                        weatherResult = Result.Success(current),
                        forecastResult = Result.Success(forecast)
                    )
                }
                _isCacheShown.value = false
            
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather for $cityToFetch", e)
                val errorMessage = ErrorHandler.handleException(e)
                
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
                                weatherResult = Result.Success(cachedWeather),
                                forecastResult = Result.Success(cachedForecast)
                            )
                        }
                        _isCacheShown.value = true
                        _cacheTimestamp.value = cachedTimestampValue
                        cacheUsed = true
                        Log.d("WeatherViewModel", "Using cached data")
                    }
                } catch (ex: Exception) {
                    Log.e("WeatherViewModel", "Error loading cache", ex)
                }
                
                if (!cacheUsed) {
                    _state.update { 
                        it.copy(
                            weatherResult = Result.Error(e),
                            forecastResult = Result.Error(e)
                        )
                    }
                    _isCacheShown.value = false
                }
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
                _state.update { 
                    it.copy(
                        weatherResult = Result.Loading,
                        forecastResult = Result.Loading
                    ) 
                }
                val currentDeferred = async { repository.getCurrentWeatherByCoords(lat, lon, units) }
                val forecastDeferred = async { repository.getForecastByCoords(lat, lon, units) }
                val current = currentDeferred.await()
                val forecast = forecastDeferred.await()
                _state.update {
                    it.copy(
                        weatherResult = Result.Success(current),
                        forecastResult = Result.Success(forecast),
                        city = current.name
                    )
                }
                preferences.saveCity(current.name)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather by location", e)
                val errorMessage = ErrorHandler.handleException(e)
                _state.update {
                    it.copy(
                        weatherResult = Result.Error(e),
                        forecastResult = Result.Error(e)
                    )
                }
            }
        }
    }

    fun onLocationError(message: String) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    weatherResult = Result.Error(Exception(message)),
                    forecastResult = Result.Error(Exception(message))
                ) 
            }
        }
    }

    fun onUnitChange(isCelsius: Boolean) {
        _state.update { it.copy(isCelsius = isCelsius) }
        viewModelScope.launch {
            try {
                preferences.saveUnitCelsius(isCelsius)
                // Перезагружаем погоду с новыми единицами измерения
                if (_state.value.city.isNotBlank()) {
                    fetchWeather()
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error saving unit", e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                preferences.clearHistory()
                _historyCities.value = emptyList()
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error clearing history", e)
            }
        }
    }

    fun removeFromHistory(city: String) {
        viewModelScope.launch {
            try {
                preferences.removeCityFromHistory(city)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error removing city from history", e)
            }
        }
    }

    fun selectCityFromHistory(city: String) {
        _state.update { it.copy(city = city) }
        fetchWeather()
    }
} 