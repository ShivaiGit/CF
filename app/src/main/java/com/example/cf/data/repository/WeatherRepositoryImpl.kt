package com.example.cf.data.repository

import android.util.Log
import com.example.cf.data.remote.WeatherApiService
import com.example.cf.domain.model.WeatherResponse
import com.example.cf.domain.model.ForecastResponse
import com.example.cf.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class WeatherRepositoryImpl @Inject constructor(
    private val apiKey: String,
    private val api: WeatherApiService
) : WeatherRepository {

    // Кэш для предотвращения повторных запросов
    private val weatherCache = mutableMapOf<String, CachedWeatherData>()
    private val forecastCache = mutableMapOf<String, CachedForecastData>()
    
    // Время жизни кэша (5 минут)
    private val cacheTimeout = 5 * 60 * 1000L
    
    // Retry настройки
    private val maxRetries = 2
    private val retryDelay = 1000L

    init {
        Log.d("WeatherRepositoryImpl", "Initializing API service with caching")
    }

    override suspend fun getCurrentWeather(city: String, units: String): WeatherResponse {
        val cacheKey = "${city}_${units}"
        
        // Проверяем кэш
        weatherCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < cacheTimeout) {
                Log.d("WeatherRepositoryImpl", "Using cached weather for: $city")
                return cached.data
            }
        }
        
        Log.d("WeatherRepositoryImpl", "Fetching current weather for city: $city, units: $units")
        
        return try {
            val response = executeWithRetry {
                withTimeout(15000) { // 15 секунд timeout
                    api.getCurrentWeather(city, apiKey, units)
                }
            }
            
            // Кэшируем результат
            weatherCache[cacheKey] = CachedWeatherData(response, System.currentTimeMillis())
            
            Log.d("WeatherRepositoryImpl", "Successfully fetched current weather for: $city")
            response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching current weather for: $city", e)
            throw e
        }
    }

    override suspend fun getForecast(city: String, units: String): ForecastResponse {
        val cacheKey = "${city}_${units}"
        
        // Проверяем кэш
        forecastCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < cacheTimeout) {
                Log.d("WeatherRepositoryImpl", "Using cached forecast for: $city")
                return cached.data
            }
        }
        
        Log.d("WeatherRepositoryImpl", "Fetching forecast for city: $city, units: $units")
        
        return try {
            val response = executeWithRetry {
                withTimeout(15000) { // 15 секунд timeout
                    api.getForecast(city, apiKey, units)
                }
            }
            
            // Кэшируем результат
            forecastCache[cacheKey] = CachedForecastData(response, System.currentTimeMillis())
            
            Log.d("WeatherRepositoryImpl", "Successfully fetched forecast for: $city")
            response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching forecast for: $city", e)
            throw e
        }
    }

    override suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double, units: String): WeatherResponse {
        val cacheKey = "${lat}_${lon}_${units}"
        
        // Проверяем кэш
        weatherCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < cacheTimeout) {
                Log.d("WeatherRepositoryImpl", "Using cached weather for coords: $lat, $lon")
                return cached.data
            }
        }
        
        Log.d("WeatherRepositoryImpl", "Fetching current weather for coords: $lat, $lon, units: $units")
        
        return try {
            val response = executeWithRetry {
                withTimeout(15000) { // 15 секунд timeout
                    api.getCurrentWeatherByCoords(lat, lon, apiKey, units)
                }
            }
            
            // Кэшируем результат
            weatherCache[cacheKey] = CachedWeatherData(response, System.currentTimeMillis())
            
            Log.d("WeatherRepositoryImpl", "Successfully fetched current weather for coords: $lat, $lon")
            response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching current weather for coords: $lat, $lon", e)
            throw e
        }
    }

    override suspend fun getForecastByCoords(lat: Double, lon: Double, units: String): ForecastResponse {
        val cacheKey = "${lat}_${lon}_${units}"
        
        // Проверяем кэш
        forecastCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < cacheTimeout) {
                Log.d("WeatherRepositoryImpl", "Using cached forecast for coords: $lat, $lon")
                return cached.data
            }
        }
        
        Log.d("WeatherRepositoryImpl", "Fetching forecast for coords: $lat, $lon, units: $units")
        
        return try {
            val response = executeWithRetry {
                withTimeout(15000) { // 15 секунд timeout
                    api.getForecastByCoords(lat, lon, apiKey, units)
                }
            }
            
            // Кэшируем результат
            forecastCache[cacheKey] = CachedForecastData(response, System.currentTimeMillis())
            
            Log.d("WeatherRepositoryImpl", "Successfully fetched forecast for coords: $lat, $lon")
            response
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error fetching forecast for coords: $lat, $lon", e)
            throw e
        }
    }
    
    // Retry логика с экспоненциальной задержкой
    private suspend fun <T> executeWithRetry(block: suspend () -> T): T {
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: TimeoutCancellationException) {
                lastException = Exception("Request timeout")
                if (attempt < maxRetries) {
                    delay(retryDelay * (attempt + 1))
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(retryDelay * (attempt + 1))
                }
            }
        }
        
        throw lastException ?: Exception("Unknown error occurred")
    }
    
    // Очистка устаревшего кэша
    fun clearExpiredCache() {
        val currentTime = System.currentTimeMillis()
        
        weatherCache.entries.removeIf { (_, cached) ->
            currentTime - cached.timestamp > cacheTimeout
        }
        
        forecastCache.entries.removeIf { (_, cached) ->
            currentTime - cached.timestamp > cacheTimeout
        }
        
        Log.d("WeatherRepositoryImpl", "Cleared expired cache")
    }
    
    // Принудительная очистка всего кэша
    fun clearAllCache() {
        weatherCache.clear()
        forecastCache.clear()
        Log.d("WeatherRepositoryImpl", "Cleared all cache")
    }
}

// Классы для кэширования данных
private data class CachedWeatherData(
    val data: WeatherResponse,
    val timestamp: Long
)

private data class CachedForecastData(
    val data: ForecastResponse,
    val timestamp: Long
) 