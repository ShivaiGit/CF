package com.example.cf

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cf.data.WeatherRepository
import com.example.cf.data.WeatherPreferences
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

class MainActivity : ComponentActivity() {
    private var showSettingsScreen = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalException", "Uncaught exception in thread ${thread.name}", throwable)
        }
        super.onCreate(savedInstanceState)

        try {
            Log.d("MainActivity", "Initializing components")
            val repository: WeatherRepository
            val preferences: WeatherPreferences
            val viewModel: WeatherViewModel
            try {
                repository = WeatherRepository("26fca3b3d5572df439654ff2f96d033d")
                Log.d("MainActivity", "Repository created")
                preferences = WeatherPreferences(this)
                Log.d("MainActivity", "Preferences created")
                viewModel = WeatherViewModel(repository, preferences)
                Log.d("MainActivity", "ViewModel created")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error creating repository, preferences, or ViewModel", e)
                throw e
            }

            Log.d("MainActivity", "Before setContent")
            setContent {
                Log.d("MainActivity", "Inside setContent")
                val state = viewModel.state.collectAsStateWithLifecycle().value
                CFTheme(darkTheme = state.isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showSettingsScreen.value) {
                            SettingsScreen(
                                isCelsius = state.isCelsius,
                                onUnitChange = { viewModel.onUnitChange(it) },
                                onBack = { showSettingsScreen.value = false }
                            )
                        } else {
                            WeatherScreen(
                                viewModel = viewModel,
                                onSettingsClick = { showSettingsScreen.value = true }
                            )
                        }
                    }
                }
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            lifecycleScope.launchWhenStarted {
                for (event in viewModel.locationEvents) {
                    when (event) {
                        is LocationEvent.RequestLocation -> {
                            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                            } else {
                                val cts = CancellationTokenSource()
                                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                                    .addOnSuccessListener { location ->
                                        if (location != null) {
                                            lifecycleScope.launch {
                                                viewModel.onLocationReceived(location.latitude, location.longitude)
                                            }
                                        } else {
                                            lifecycleScope.launch {
                                                viewModel.onLocationError("Не удалось получить координаты")
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        lifecycleScope.launch {
                                            viewModel.onLocationError("Ошибка получения координат: ${it.message}")
                                        }
                                    }
                            }
                        }
                        else -> {}
                    }
                }
            }

            Log.d("MainActivity", "Setup completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization", e)
            // throw e // Не завершаем Activity, только логируем ошибку
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called")
    }
}