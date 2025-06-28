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
import com.example.cf.domain.model.ForecastItem
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
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.cf.R

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
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Получаем список уникальных дней для прогноза
    val dailyForecasts = remember(state.forecast) {
        state.forecast?.list?.groupBy { item ->
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
            items.minByOrNull { item ->
                val calendar = Calendar.getInstance().apply {
                    time = Date(item.dt * 1000)
                }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                Math.abs(hour - 12)
            } ?: items.first()
        }?.take(5)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ошибка показывается через Text
        if (state.error != null) {
            Text(
                text = state.error ?: "",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                label = { Text("") },
                placeholder = { Text(stringResource(R.string.placeholder_city)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.onMyLocationClick() },
                enabled = !state.isLoading,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.my_location)
                )
            }
            IconButton(
                onClick = viewModel::fetchWeather,
                enabled = !state.isLoading && state.city.isNotBlank(),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
        if (historyCities.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                historyCities.forEach { city ->
                    AssistChip(
                        onClick = { viewModel.selectCityFromHistory(city) },
                        label = { Text(city) },
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_history)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LoadingAnimation(
                modifier = Modifier.padding(4.dp)
            )
        }
        AnimatedVisibility(
            visible = state.weather != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = screenHeight * 0.55f)
            ) {
                if (isCacheShown) {
                    val formattedTime = cacheTimestamp?.let {
                        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: "?"
                    Text(
                        text = stringResource(R.string.outdated_data) + "\n" + stringResource(R.string.last_update, formattedTime),
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
                        val unitStr = if (state.isCelsius) stringResource(R.string.unit_celsius) else stringResource(R.string.unit_fahrenheit)
                        val shareLabel = stringResource(R.string.share_weather)
                        IconButton(
                            onClick = {
                                val desc = weather.weather.firstOrNull()?.description?.capitalize() ?: ""
                                val temp = weather.main.temp
                                val city = weather.name
                                val shareText = city + ": " + temp + unitStr + ", " + desc
                                onShareWeather(shareText)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = shareLabel
                            )
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .weight(1f, fill = true),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize()
                        ) {
                            Text(
                                text = weather.name,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            weather.weather.firstOrNull()?.let { weatherInfo ->
                                WeatherIcon(
                                    iconCode = weatherInfo.icon,
                                    modifier = Modifier.size(120.dp),
                                    contentDescription = weatherInfo.description
                                )
                            }
                            Text(
                                text = "${weather.main.temp}°$unit",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = stringResource(R.string.feels_like, weather.main.feels_like, unit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = stringResource(R.string.min_max, weather.main.temp_min, weather.main.temp_max, unit),
                                style = MaterialTheme.typography.bodySmall,
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
                                    icon = Icons.Default.WaterDrop,
                                    value = stringResource(R.string.humidity, weather.main.humidity)
                                )
                                WeatherInfoItem(
                                    icon = Icons.Default.Air,
                                    value = stringResource(R.string.wind_speed, weather.wind.speed)
                                )
                                WeatherInfoItem(
                                    icon = Icons.Default.Speed,
                                    value = stringResource(R.string.pressure, weather.main.pressure)
                                )
                                WeatherInfoItem(
                                    icon = Icons.Default.Cloud,
                                    value = stringResource(R.string.cloudiness, weather.clouds.all)
                                )
                                WeatherInfoItem(
                                    icon = Icons.Default.Visibility,
                                    value = stringResource(R.string.visibility, weather.visibility / 1000.0)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val sunrise = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(weather.sys.sunrise * 1000))
                                val sunset = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(weather.sys.sunset * 1000))
                                Text(stringResource(R.string.sunrise, sunrise), style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.sunset, sunset), style = MaterialTheme.typography.bodySmall)
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
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.forecast_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
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
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ForecastCard(forecast: ForecastItem, unit: String) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(220.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(6.dp)
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

            Spacer(modifier = Modifier.height(1.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 2.dp, vertical = 1.dp)
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
                    modifier = Modifier.size(56.dp),
                    contentDescription = weather.description
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${forecast.main.temp}°$unit",
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 