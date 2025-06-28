package com.example.cf

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cf.ui.theme.CFTheme
import com.example.cf.ui.weather.WeatherScreen
import com.example.cf.ui.weather.WeatherViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationTokenSource
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.cf.ui.weather.LocationEvent
import com.example.cf.ui.weather.SettingsScreen
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.viewModels
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // Оптимизируем состояние с помощью ленивой инициализации
    private var showSettingsScreen by mutableStateOf(false)
    
    // Ленивая инициализация ViewModel
    private val viewModel: WeatherViewModel by viewModels()
    
    // Ленивая инициализация LocationClient
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    
    // Кэш для разрешений
    private var locationPermissionGranted = false
    private var lastLocationRequestTime = 0L
    private val locationRequestCooldown = 5000L // 5 секунд между запросами

    override fun onCreate(savedInstanceState: Bundle?) {
        // Глобальный обработчик исключений
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalException", "Uncaught exception in thread ${thread.name}", throwable)
        }
        
        super.onCreate(savedInstanceState)

        try {
            Log.d("MainActivity", "Initializing components")

            // Оптимизированная установка контента
            setContent {
                Log.d("MainActivity", "Starting setContent")
                
                CFTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Оптимизируем состояние с помощью remember
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        Log.d("MainActivity", "State collected: city=${state.city}, isLoading=${state.isLoading}")
                        
                        if (showSettingsScreen) {
                            SettingsScreen(
                                isCelsius = state.isCelsius,
                                onUnitChange = { viewModel.toggleTemperatureUnit() },
                                onBack = { showSettingsScreen = false }
                            )
                        } else {
                            WeatherScreen(
                                viewModel = viewModel,
                                onSettingsClick = { showSettingsScreen = true }
                            )
                        }
                    }
                }
                Log.d("MainActivity", "setContent completed successfully")
            }

            // Оптимизированная настройка обработки местоположения
            setupLocationHandling()

            Log.d("MainActivity", "Setup completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization", e)
            throw e
        }
    }

    private fun setupLocationHandling() {
        try {
            Log.d("MainActivity", "Setting up location handling")
            
            lifecycleScope.launch {
                try {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        Log.d("MainActivity", "Location lifecycle started")
                        
                        // Оптимизируем обработку событий с помощью collectLatest
                        viewModel.locationEvents.collectLatest { event ->
                            Log.d("MainActivity", "Received location event: $event")
                            
                            when (event) {
                                is LocationEvent.RequestLocation -> {
                                    handleLocationRequest()
                                }
                                else -> {
                                    // Обрабатываем другие события если нужно
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in location handling", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up location handling", e)
        }
    }
    
    private fun handleLocationRequest() {
        val currentTime = System.currentTimeMillis()
        
        // Проверяем cooldown для предотвращения спама запросов
        if (currentTime - lastLocationRequestTime < locationRequestCooldown) {
            Log.d("MainActivity", "Location request ignored due to cooldown")
            return
        }
        
        lastLocationRequestTime = currentTime
        
        // Проверяем разрешения
        if (!locationPermissionGranted) {
            if (ActivityCompat.checkSelfPermission(
                    this, 
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, 
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 
                    1001
                )
                return
            } else {
                locationPermissionGranted = true
            }
        }
        
        // Получаем местоположение
        requestLocation()
    }
    
    private fun requestLocation() {
        try {
            val cts = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 
                cts.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("MainActivity", "Location received: ${location.latitude}, ${location.longitude}")
                    lifecycleScope.launch {
                        // Отправляем результат в ViewModel
                        viewModel.locationEvents.trySend(
                            LocationEvent.LocationResult(location.latitude, location.longitude)
                        )
                    }
                } else {
                    Log.w("MainActivity", "Location is null")
                    lifecycleScope.launch {
                        viewModel.locationEvents.trySend(
                            LocationEvent.Error("Не удалось получить координаты")
                        )
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("MainActivity", "Location request failed", exception)
                lifecycleScope.launch {
                    viewModel.locationEvents.trySend(
                        LocationEvent.Error("Ошибка получения координат: ${exception.message}")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error requesting location", e)
            lifecycleScope.launch {
                viewModel.locationEvents.trySend(
                    LocationEvent.Error("Ошибка запроса местоположения")
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Location permission granted")
                    locationPermissionGranted = true
                    // Повторяем запрос местоположения
                    requestLocation()
                } else {
                    Log.w("MainActivity", "Location permission denied")
                    lifecycleScope.launch {
                        viewModel.locationEvents.trySend(
                            LocationEvent.Error("Разрешение на местоположение не предоставлено")
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called")
        
        // Очищаем ресурсы
        try {
            // Очищаем кэш если нужно
            locationPermissionGranted = false
            lastLocationRequestTime = 0L
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during cleanup", e)
        }
    }
}