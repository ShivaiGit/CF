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

class MainActivity : ComponentActivity() {
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
                CFTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WeatherScreen(viewModel = viewModel)
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