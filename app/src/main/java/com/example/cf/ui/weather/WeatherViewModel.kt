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
import kotlinx.coroutines.delay

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
    
    // Оптимизируем состояние с помощью MutableStateFlow
    private val _state = MutableStateFlow(WeatherState())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    private val _locationEvents = Channel<LocationEvent>(Channel.BUFFERED)
    val locationEvents: ReceiveChannel<LocationEvent> = _locationEvents

    private val _isCacheShown = MutableStateFlow(false)
    val isCacheShown: StateFlow<Boolean> = _isCacheShown.asStateFlow()

    private val _cacheTimestamp = MutableStateFlow<Long?>(null)
    val cacheTimestamp: StateFlow<Long?> = _cacheTimestamp.asStateFlow()

    // Оптимизируем историю городов с кэшированием
    private val _historyCities = MutableStateFlow<List<String>>(emptyList())
    val historyCities: StateFlow<List<String>> = _historyCities.asStateFlow()

    // Кэш для предотвращения повторных запросов
    private var lastFetchCity = ""
    private var lastFetchUnits = ""
    private var isFetching = false

    init {
        Log.d("WeatherViewModel", "Initializing ViewModel")
        try {
            loadSavedCity()
            setupHistoryCitiesFlow()
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error in init", e)
        }
    }

    private fun setupHistoryCitiesFlow() {
        viewModelScope.launch {
            try {
                preferences.historyCities
                    .distinctUntilChanged() // Оптимизация: только при изменении
                    .collect {
                        Log.d("WeatherViewModel", "History cities updated: $it")
                        _historyCities.value = it
                    }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error collecting history cities", e)
            }
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

    // Оптимизированная функция изменения города с debounce
    fun onCityChange(newCity: String) {
        Log.d("WeatherViewModel", "City changed to: '$newCity'")
        _state.update { it.copy(city = newCity) }
        
        // Автоматический поиск с debounce для оптимизации
        viewModelScope.launch {
            delay(500) // Debounce 500ms
            if (_state.value.city == newCity && newCity.isNotBlank()) {
                fetchWeather()
            }
        }
    }

    private fun clearSavedCity() {
        viewModelScope.launch {
            try {
                Log.d("WeatherViewModel", "Clearing saved city")
                preferences.saveCity("")
                _state.update { 
                    it.copy(
                        city = "", 
                        weatherResult = Result.Loading, 
                        forecastResult = Result.Loading 
                    ) 
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error clearing saved city", e)
            }
        }
    }

    // Оптимизированная функция загрузки погоды с кэшированием
    fun fetchWeather(isAutoLoad: Boolean = false) {
        val cityToFetch = _state.value.city
        val units = if (_state.value.isCelsius) "metric" else "imperial"
        
        Log.d("WeatherViewModel", "fetchWeather called for city: '$cityToFetch', isAutoLoad: $isAutoLoad")
        
        if (cityToFetch.isBlank()) {
            Log.d("WeatherViewModel", "Attempted to fetch weather with blank city")
            return
        }

        // Проверяем кэш для предотвращения повторных запросов
        if (isFetching && lastFetchCity == cityToFetch && lastFetchUnits == units) {
            Log.d("WeatherViewModel", "Already fetching weather for $cityToFetch")
            return
        }

        isFetching = true
        lastFetchCity = cityToFetch
        lastFetchUnits = units

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
                val currentDeferred = async { 
                    repository.getCurrentWeather(cityToFetch, units) 
                }
                val forecastDeferred = async { 
                    repository.getForecast(cityToFetch, units) 
                }

                val current = currentDeferred.await()
                val forecast = forecastDeferred.await()

                Log.d("WeatherViewModel", "Successfully fetched weather for: $cityToFetch")
                
                // Сохраняем город ТОЛЬКО после успешного получения данных
                preferences.saveCity(cityToFetch)
                // Добавляем в историю
                preferences.addCityToHistory(cityToFetch)

                // Кэшируем данные
                cacheWeatherData(current, forecast)

                _state.update { 
                    it.copy(
                        weatherResult = Result.Success(current),
                        forecastResult = Result.Success(forecast)
                    )
                }
                _isCacheShown.value = false
            
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather for $cityToFetch", e)
                handleFetchError(e, isAutoLoad)
            } finally {
                isFetching = false
            }
        }
    }

    private suspend fun cacheWeatherData(current: WeatherResponse, forecast: ForecastResponse) {
        try {
            preferences.saveCachedWeather(gson.toJson(current))
            preferences.saveCachedForecast(gson.toJson(forecast))
            val timestamp = System.currentTimeMillis()
            preferences.saveCachedTimestamp(timestamp)
            _cacheTimestamp.value = timestamp
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error caching weather/forecast", e)
        }
    }

    private suspend fun handleFetchError(e: Exception, isAutoLoad: Boolean) {
        val errorMessage = ErrorHandler.handleException(e)
        
        // Если это автозагрузка и возникла ошибка - очищаем сохранённый город
        if (isAutoLoad) {
            Log.d("WeatherViewModel", "Auto-load failed, clearing saved city")
            clearSavedCity()
        }
        
        // Пробуем загрузить кэш
        if (!loadCachedData()) {
            _state.update { 
                it.copy(
                    weatherResult = Result.Error(e),
                    forecastResult = Result.Error(e)
                )
            }
            _isCacheShown.value = false
        }
    }

    private suspend fun loadCachedData(): Boolean {
        return try {
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
                Log.d("WeatherViewModel", "Using cached data")
                true
            } else {
                false
            }
        } catch (ex: Exception) {
            Log.e("WeatherViewModel", "Error loading cache", ex)
            false
        }
    }

    fun onMyLocationClick() {
        Log.d("WeatherViewModel", "My location button clicked")
        _locationEvents.trySend(LocationEvent.RequestLocation)
    }

    fun selectCityFromHistory(city: String) {
        Log.d("WeatherViewModel", "City selected from history: $city")
        _state.update { it.copy(city = city) }
        fetchWeather()
    }

    fun toggleTemperatureUnit() {
        val currentState = _state.value
        val newIsCelsius = !currentState.isCelsius
        
        _state.update { it.copy(isCelsius = newIsCelsius) }
        
        // Если есть город, перезагружаем данные с новыми единицами
        if (currentState.city.isNotBlank()) {
            fetchWeather()
        }
    }

    fun clearError() {
        _state.update { 
            it.copy(
                weatherResult = Result.Loading,
                forecastResult = Result.Loading
            ) 
        }
    }

    // Методы для обработки событий местоположения
    fun onLocationReceived(lat: Double, lon: Double) {
        Log.d("WeatherViewModel", "Location received: $lat, $lon")
        viewModelScope.launch {
            try {
                val units = if (_state.value.isCelsius) "metric" else "imperial"
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
                preferences.addCityToHistory(current.name)
                
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
        Log.d("WeatherViewModel", "Location error: $message")
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    weatherResult = Result.Error(Exception(message)),
                    forecastResult = Result.Error(Exception(message))
                ) 
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _locationEvents.close()
        Log.d("WeatherViewModel", "ViewModel cleared")
    }
} 