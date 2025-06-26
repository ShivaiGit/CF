package com.example.cf

import android.os.Bundle
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
        super.onCreate(savedInstanceState)

        // TODO: Move to DI
        val repository = WeatherRepository("26fca3b3d5572df439654ff2f96d033d")
        val preferences = WeatherPreferences(this)
        val viewModel = WeatherViewModel(repository, preferences)

        setContent {
            CFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen(viewModel = viewModel)
                }
            }
        }
    }
}