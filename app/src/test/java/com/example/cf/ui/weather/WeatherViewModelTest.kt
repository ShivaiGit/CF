package com.example.cf.ui.weather

import app.cash.turbine.test
import com.example.cf.core.Result
import com.example.cf.data.ForecastResponse
import com.example.cf.data.WeatherPreferences
import com.example.cf.data.WeatherRepository
import com.example.cf.data.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private lateinit var viewModel: WeatherViewModel
    private lateinit var repository: WeatherRepository
    private lateinit var preferences: WeatherPreferences
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        preferences = mock()
        viewModel = WeatherViewModel(repository, preferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when fetchWeather succeeds, state updates with weather data`() = runTest {
        // Given
        val city = "Moscow"
        val weatherResponse = createMockWeatherResponse(city)
        val forecastResponse = createMockForecastResponse()
        
        whenever(repository.getCurrentWeather(city, "metric")).thenReturn(weatherResponse)
        whenever(repository.getForecast(city, "metric")).thenReturn(forecastResponse)
        whenever(preferences.saveCity(city)).thenAnswer { }
        whenever(preferences.addCityToHistory(city)).thenAnswer { }
        whenever(preferences.saveCachedWeather(any())).thenAnswer { }
        whenever(preferences.saveCachedForecast(any())).thenAnswer { }
        whenever(preferences.saveCachedTimestamp(any())).thenAnswer { }

        // When
        viewModel.onCityChange(city)
        viewModel.fetchWeather()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.weatherResult.isSuccess())
            assertTrue(state.forecastResult.isSuccess())
            assertEquals(weatherResponse, state.weather)
            assertEquals(forecastResponse, state.forecast)
            assertEquals(city, state.city)
        }

        verify(repository).getCurrentWeather(city, "metric")
        verify(repository).getForecast(city, "metric")
        verify(preferences).saveCity(city)
        verify(preferences).addCityToHistory(city)
    }

    @Test
    fun `when fetchWeather fails, state updates with error`() = runTest {
        // Given
        val city = "InvalidCity"
        val exception = IOException("Network error")
        
        whenever(repository.getCurrentWeather(city, "metric")).thenThrow(exception)
        whenever(repository.getForecast(city, "metric")).thenThrow(exception)
        whenever(preferences.cachedWeather).thenReturn(flowOf(null))
        whenever(preferences.cachedForecast).thenReturn(flowOf(null))

        // When
        viewModel.onCityChange(city)
        viewModel.fetchWeather()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.weatherResult.isError())
            assertTrue(state.forecastResult.isError())
            assertEquals(exception, state.weatherResult.exceptionOrNull())
            assertEquals(exception, state.forecastResult.exceptionOrNull())
        }
    }

    @Test
    fun `when fetchWeather fails but cache exists, shows cached data`() = runTest {
        // Given
        val city = "Moscow"
        val cachedWeather = createMockWeatherResponse(city)
        val cachedForecast = createMockForecastResponse()
        val exception = IOException("Network error")
        
        whenever(repository.getCurrentWeather(city, "metric")).thenThrow(exception)
        whenever(repository.getForecast(city, "metric")).thenThrow(exception)
        whenever(preferences.cachedWeather).thenReturn(flowOf("cached_weather_json"))
        whenever(preferences.cachedForecast).thenReturn(flowOf("cached_forecast_json"))
        whenever(preferences.cachedTimestamp).thenReturn(flowOf(123456789L))

        // When
        viewModel.onCityChange(city)
        viewModel.fetchWeather()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.isCacheShown.test {
            val isCacheShown = awaitItem()
            assertTrue(isCacheShown)
        }
    }

    @Test
    fun `when city is blank, fetchWeather does nothing`() = runTest {
        // When
        viewModel.fetchWeather()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(repository, never()).getCurrentWeather(any(), any())
        verify(repository, never()).getForecast(any(), any())
    }

    @Test
    fun `onCityChange updates city in state`() = runTest {
        // Given
        val newCity = "London"

        // When
        viewModel.onCityChange(newCity)

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(newCity, state.city)
        }
    }

    @Test
    fun `toggleTheme updates theme in state and preferences`() = runTest {
        // Given
        whenever(preferences.saveDarkTheme(true)).thenAnswer { }

        // When
        viewModel.toggleTheme()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isDarkTheme)
        }
        verify(preferences).saveDarkTheme(true)
    }

    @Test
    fun `onUnitChange updates unit and refetches weather`() = runTest {
        // Given
        val city = "Moscow"
        val weatherResponse = createMockWeatherResponse(city)
        val forecastResponse = createMockForecastResponse()
        
        whenever(repository.getCurrentWeather(city, "imperial")).thenReturn(weatherResponse)
        whenever(repository.getForecast(city, "imperial")).thenReturn(forecastResponse)
        whenever(preferences.saveUnitCelsius(false)).thenAnswer { }
        whenever(preferences.saveCity(city)).thenAnswer { }
        whenever(preferences.addCityToHistory(city)).thenAnswer { }

        // When
        viewModel.onCityChange(city)
        viewModel.onUnitChange(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isCelsius)
        }
        verify(preferences).saveUnitCelsius(false)
        verify(repository).getCurrentWeather(city, "imperial")
        verify(repository).getForecast(city, "imperial")
    }

    @Test
    fun `clearHistory calls preferences clearHistory`() = runTest {
        // Given
        whenever(preferences.clearHistory()).thenAnswer { }

        // When
        viewModel.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(preferences).clearHistory()
    }

    @Test
    fun `removeFromHistory calls preferences removeCityFromHistory`() = runTest {
        // Given
        val city = "Moscow"
        whenever(preferences.removeCityFromHistory(city)).thenAnswer { }

        // When
        viewModel.removeFromHistory(city)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(preferences).removeCityFromHistory(city)
    }

    @Test
    fun `selectCityFromHistory updates city and fetches weather`() = runTest {
        // Given
        val city = "London"
        val weatherResponse = createMockWeatherResponse(city)
        val forecastResponse = createMockForecastResponse()
        
        whenever(repository.getCurrentWeather(city, "metric")).thenReturn(weatherResponse)
        whenever(repository.getForecast(city, "metric")).thenReturn(forecastResponse)
        whenever(preferences.saveCity(city)).thenAnswer { }
        whenever(preferences.addCityToHistory(city)).thenAnswer { }

        // When
        viewModel.selectCityFromHistory(city)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(city, state.city)
        }
        verify(repository).getCurrentWeather(city, "metric")
        verify(repository).getForecast(city, "metric")
    }

    private fun createMockWeatherResponse(city: String): WeatherResponse {
        return WeatherResponse(
            weather = listOf(
                com.example.cf.data.Weather("Clear", "clear sky", "01d")
            ),
            main = com.example.cf.data.Main(20.0, 18.0, 15.0, 25.0, 65, 1013),
            wind = com.example.cf.data.Wind(5.0),
            sys = com.example.cf.data.Sys(1640995200, 1641038400),
            clouds = com.example.cf.data.Clouds(20),
            visibility = 10000,
            name = city
        )
    }

    private fun createMockForecastResponse(): ForecastResponse {
        return ForecastResponse(
            list = listOf(
                com.example.cf.data.ForecastItem(
                    dt = 1640995200,
                    main = com.example.cf.data.Main(20.0, 18.0, 15.0, 25.0, 65, 1013),
                    weather = listOf(
                        com.example.cf.data.Weather("Clear", "clear sky", "01d")
                    ),
                    wind = com.example.cf.data.Wind(5.0)
                )
            )
        )
    }
} 