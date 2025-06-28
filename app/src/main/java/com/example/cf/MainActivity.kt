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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var showSettingsScreen = mutableStateOf(false)
    
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalException", "Uncaught exception in thread ${thread.name}", throwable)
        }
        super.onCreate(savedInstanceState)

        try {
            Log.d("MainActivity", "Initializing components")

            setContent {
                Log.d("MainActivity", "Starting setContent")
                val state = viewModel.state.collectAsStateWithLifecycle().value
                Log.d("MainActivity", "State collected: city=${state.city}, isLoading=${state.isLoading}")
                
                CFTheme {
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
                Log.d("MainActivity", "setContent completed successfully")
            }

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
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            lifecycleScope.launch {
                try {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        Log.d("MainActivity", "Location lifecycle started")
                        for (event in viewModel.locationEvents) {
                            Log.d("MainActivity", "Received location event: $event")
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
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in location handling", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up location handling", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called")
    }
}