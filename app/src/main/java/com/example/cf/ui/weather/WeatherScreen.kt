package com.example.cf.ui.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cf.data.ForecastItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search field
        OutlinedTextField(
            value = state.city,
            onValueChange = viewModel::onCityChange,
            label = { Text("Enter city name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search button
        Button(
            onClick = viewModel::fetchWeather,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.city.isNotBlank()
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Loading indicator
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        // Error message
        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Current Weather
        state.weather?.let { weather ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = weather.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${weather.main.temp}°C",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = weather.weather.firstOrNull()?.description?.capitalize() ?: "",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Humidity: ${weather.main.humidity}%")
                    Text("Wind: ${weather.wind.speed} m/s")
                }
            }
        }

        // 5-day Forecast
        state.forecast?.let { forecast ->
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "5-Day Forecast",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(forecast.list.take(5)) { item ->
                    ForecastCard(item)
                }
            }
        }
    }
}

@Composable
fun ForecastCard(forecast: ForecastItem) {
    Card(
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Format date
            val date = SimpleDateFormat("EEE", Locale.getDefault())
                .format(Date(forecast.dt * 1000))
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${forecast.main.temp}°C",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = forecast.weather.firstOrNull()?.main ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${forecast.wind.speed} m/s",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 