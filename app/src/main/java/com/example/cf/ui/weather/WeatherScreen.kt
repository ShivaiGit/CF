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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Получаем список уникальных дней для прогноза
    val dailyForecasts = remember(state.forecast) {
        state.forecast?.list?.groupBy { item ->
            // Конвертируем timestamp в начало дня (00:00)
            val date = Date(item.dt * 1000)
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.time
        }?.map { (_, items) ->
            // Берем прогноз на середину дня (около 12:00)
            items.minByOrNull { item ->
                val calendar = Calendar.getInstance().apply {
                    time = Date(item.dt * 1000)
                }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                Math.abs(hour - 12) // Находим время, ближайшее к полудню
            } ?: items.first()
        }?.take(5) // Берем только 5 дней
    }

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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = weather.name,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    weather.weather.firstOrNull()?.let { weatherInfo ->
                        WeatherIcon(
                            iconCode = weatherInfo.icon,
                            modifier = Modifier.size(100.dp),
                            contentDescription = weatherInfo.description
                        )
                    }
                    
                    Text(
                        text = "${weather.main.temp}°C",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = weather.weather.firstOrNull()?.description?.capitalize() ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WeatherInfoItem(
                            title = "Humidity",
                            value = "${weather.main.humidity}%"
                        )
                        WeatherInfoItem(
                            title = "Wind",
                            value = "${weather.wind.speed} m/s"
                        )
                    }
                }
            }
        }

        // 5-day Forecast
        if (!dailyForecasts.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "5-Day Forecast",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(dailyForecasts) { item ->
                    ForecastCard(item)
                }
            }
        }
    }
}

@Composable
fun WeatherInfoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun ForecastCard(forecast: ForecastItem) {
    Card(
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Format date
            val dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            val date = dateFormatter.format(Date(forecast.dt * 1000))
            
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            forecast.weather.firstOrNull()?.let { weather ->
                WeatherIcon(
                    iconCode = weather.icon,
                    modifier = Modifier.size(50.dp),
                    contentDescription = weather.description
                )
            }
            
            Text(
                text = "${forecast.main.temp}°C",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = forecast.weather.firstOrNull()?.main ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 