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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import android.util.Log
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp * scale)
                .align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onShareWeather: (String) -> Unit = {}
) {
    Log.d("WeatherScreen", "Composing WeatherScreen, city: ${viewModel.state.value.city}, error: ${viewModel.state.value.error}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val unit = if (state.isCelsius) "C" else "F"
    val isCacheShown by viewModel.isCacheShown.collectAsStateWithLifecycle()
    val cacheTimestamp by viewModel.cacheTimestamp.collectAsStateWithLifecycle()
    val historyCities by viewModel.historyCities.collectAsStateWithLifecycle()

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
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Погода") },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                }
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                label = { Text("Enter city name") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { viewModel.onMyLocationClick() },
                enabled = !state.isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Моё местоположение"
                )
            }
        }
        if (historyCities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    historyCities.forEach { city ->
                        AssistChip(
                            onClick = { viewModel.selectCityFromHistory(city) },
                            label = { Text(city) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Очистить историю"
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Dark theme",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Switch(
                    checked = state.isDarkTheme,
                    onCheckedChange = { viewModel.toggleTheme() },
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = viewModel::fetchWeather,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.city.isNotBlank()
        ) {
            AnimatedContent(
                targetState = state.isLoading,
                transitionSpec = {
                    fadeIn() + slideInVertically { -it } togetherWith
                    fadeOut() + slideOutVertically { it }
                },
                label = "loading"
            ) { isLoading ->
                Text(if (isLoading) "Searching..." else "Search")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LoadingAnimation(
                modifier = Modifier.padding(8.dp)
            )
        }
        AnimatedVisibility(
            visible = state.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = state.weather != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isCacheShown) {
                    val formattedTime = cacheTimestamp?.let {
                        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: "?"
                    Text(
                        text = "Данные устаревшие (нет интернета)\nПоследнее обновление: $formattedTime",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )
                }
                state.weather?.let { weather ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                val desc = weather.weather.firstOrNull()?.description?.capitalize() ?: ""
                                val temp = weather.main.temp
                                val city = weather.name
                                val unitStr = if (state.isCelsius) "°C" else "°F"
                                val shareText = "Погода в $city: $temp$unitStr, $desc"
                                onShareWeather(shareText)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Поделиться погодой"
                            )
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = weather.name,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            weather.weather.firstOrNull()?.let { weatherInfo ->
                                WeatherIcon(
                                    iconCode = weatherInfo.icon,
                                    modifier = Modifier.size(80.dp),
                                    contentDescription = weatherInfo.description
                                )
                            }
                            Text(
                                text = "${weather.main.temp}°$unit",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "Ощущается как: ${weather.main.feels_like}°$unit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = weather.weather.firstOrNull()?.description?.capitalize() ?: "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                                WeatherInfoItem(
                                    title = "Pressure",
                                    value = "${weather.main.pressure} hPa"
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val sunrise = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(weather.sys.sunrise * 1000))
                                val sunset = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(weather.sys.sunset * 1000))
                                Text("Восход: $sunrise", style = MaterialTheme.typography.bodySmall)
                                Text("Закат: $sunset", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = !dailyForecasts.isNullOrEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (!dailyForecasts.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "5-Day Forecast",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(start = 8.dp, bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            items(dailyForecasts) { item ->
                                ForecastCard(item, unit)
                            }
                        }
                    }
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
fun ForecastCard(forecast: ForecastItem, unit: String) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(260.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            val date = dateFormatter.format(Date(forecast.dt * 1000))

            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = forecast.weather.firstOrNull()?.main ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            forecast.weather.firstOrNull()?.let { weather ->
                WeatherIcon(
                    iconCode = weather.icon,
                    modifier = Modifier.size(40.dp),
                    contentDescription = weather.description
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${forecast.main.temp}°$unit",
                style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 