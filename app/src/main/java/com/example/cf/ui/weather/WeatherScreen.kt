package com.example.cf.ui.weather

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cf.R
import com.example.cf.domain.model.ForecastItem
import com.example.cf.domain.model.Weather
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "loading")
    
    // Анимация масштабирования
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Анимация прозрачности
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Анимация вращения
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Основной индикатор загрузки
        CircularProgressIndicator(
            modifier = Modifier
                .size(60.dp * scale)
                .graphicsLayer(
                    alpha = alpha,
                    rotationZ = rotation
                ),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        
        // Дополнительные круги для эффекта
        repeat(3) { index ->
            val delay = index * 200
            val delayedTransition = rememberInfiniteTransition(label = "delayed_$index")
            val delayedScale by delayedTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "delayed_scale_$index"
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp * delayedScale)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onShareWeather: (String) -> Unit = {}
) {
    Log.d("WeatherScreen", "Composing WeatherScreen")
    
    // Собираем все состояния в одном месте для оптимизации
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isCacheShown by viewModel.isCacheShown.collectAsStateWithLifecycle()
    val cacheTimestamp by viewModel.cacheTimestamp.collectAsStateWithLifecycle()
    val historyCities by viewModel.historyCities.collectAsStateWithLifecycle()
    
    Log.d("WeatherScreen", "States collected: city=${state.city}, isLoading=${state.isLoading}, weather=${state.weather != null}")
    
    val unit = if (state.isCelsius) "C" else "F"
    val configuration = LocalConfiguration.current

    // Получаем строки ресурсов в начале функции
    val myLocationText = stringResource(R.string.my_location)
    val settingsText = stringResource(R.string.settings)
    val outdatedDataText = stringResource(R.string.outdated_data)
    val lastUpdateText = stringResource(R.string.last_update)
    val celsiusText = stringResource(R.string.unit_celsius)
    val fahrenheitText = stringResource(R.string.unit_fahrenheit)
    val shareWeatherText = stringResource(R.string.share_weather)
    val feelsLikeText = stringResource(R.string.feels_like)
    val minMaxText = stringResource(R.string.min_max)
    val humidityText = stringResource(R.string.humidity)
    val windSpeedText = stringResource(R.string.wind_speed)
    val pressureText = stringResource(R.string.pressure)
    val cloudinessText = stringResource(R.string.cloudiness)
    val visibilityText = stringResource(R.string.visibility)
    val sunriseText = stringResource(R.string.sunrise)
    val sunsetText = stringResource(R.string.sunset)
    val forecastTitleText = stringResource(R.string.forecast_title)
    val placeholderText = stringResource(R.string.enter_city_name)
    val clearText = stringResource(R.string.clear)

    // Pull-to-refresh состояние
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = {
            if (state.city.isNotBlank()) {
                viewModel.fetchWeather()
            }
        }
    )

    // Оптимизируем remember для dailyForecasts
    val dailyForecasts = remember(state.forecast?.list) {
        Log.d("WeatherScreen", "Calculating dailyForecasts")
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

    Log.d("WeatherScreen", "dailyForecasts size: ${dailyForecasts?.size}")

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // Ошибка показывается через Text с анимацией
            item {
                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Поисковая панель с улучшенным дизайном
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Поле поиска города с историей
                        AnimatedSearchField(
                            value = state.city,
                            onValueChange = viewModel::onCityChange,
                            onSearch = viewModel::fetchWeather,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                            placeholderText = placeholderText,
                            clearText = clearText,
                            historyCities = historyCities,
                            onHistoryItemClick = { viewModel.selectCityFromHistory(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Панель с кнопками
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Кнопка "Моё местоположение"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                AnimatedIconButton(
                                    onClick = { viewModel.onMyLocationClick() },
                                    enabled = !state.isLoading,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = myLocationText,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Моё место",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Кнопка "Настройки"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                AnimatedIconButton(
                                    onClick = onSettingsClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings, 
                                            contentDescription = settingsText,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Настройки",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Показываем загрузку только если действительно загружаемся и город не пустой
            item {
                AnimatedVisibility(
                    visible = state.isLoading && state.city.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Log.d("WeatherScreen", "Showing loading animation")
                    LoadingAnimation(
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Показываем погоду только если она есть
            item {
                AnimatedVisibility(
                    visible = state.weather != null,
                    enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    state.weather?.let { weather ->
                        Log.d("WeatherScreen", "Showing weather for: ${weather.name}")
                        WeatherCard(
                            weather = weather,
                            unit = unit,
                            isCacheShown = isCacheShown,
                            cacheTimestamp = cacheTimestamp,
                            onShareWeather = onShareWeather,
                            outdatedDataText = outdatedDataText,
                            lastUpdateText = lastUpdateText,
                            celsiusText = celsiusText,
                            fahrenheitText = fahrenheitText,
                            shareWeatherText = shareWeatherText,
                            feelsLikeText = feelsLikeText,
                            minMaxText = minMaxText,
                            humidityText = humidityText,
                            windSpeedText = windSpeedText,
                            pressureText = pressureText,
                            cloudinessText = cloudinessText,
                            visibilityText = visibilityText,
                            sunriseText = sunriseText,
                            sunsetText = sunsetText
                        )
                    }
                }
            }
            
            // Показываем прогноз только если он есть
            item {
                AnimatedVisibility(
                    visible = !dailyForecasts.isNullOrEmpty(),
                    enter = fadeIn(animationSpec = tween(700)) + expandVertically(animationSpec = tween(700)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    if (!dailyForecasts.isNullOrEmpty()) {
                        Log.d("WeatherScreen", "Showing forecast with ${dailyForecasts.size} items")
                        ForecastSection(
                            dailyForecasts = dailyForecasts, 
                            unit = unit,
                            forecastTitleText = forecastTitleText
                        )
                    }
                }
            }
        }
        
        // Pull-to-refresh индикатор
        PullRefreshIndicator(
            refreshing = state.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
    
    Log.d("WeatherScreen", "WeatherScreen composition completed")
}

@Composable
fun WeatherCard(
    weather: com.example.cf.domain.model.WeatherResponse,
    unit: String,
    isCacheShown: Boolean,
    cacheTimestamp: Long?,
    onShareWeather: (String) -> Unit,
    outdatedDataText: String,
    lastUpdateText: String,
    celsiusText: String,
    fahrenheitText: String,
    shareWeatherText: String,
    feelsLikeText: String,
    minMaxText: String,
    humidityText: String,
    windSpeedText: String,
    pressureText: String,
    cloudinessText: String,
    visibilityText: String,
    sunriseText: String,
    sunsetText: String
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                alpha = if (visible) 1f else 0f
                translationY = if (visible) 0f else 50f
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Кэш уведомление
            if (isCacheShown) {
                val formattedTime = cacheTimestamp?.let {
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(it))
                } ?: "?"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = outdatedDataText + "\n" + lastUpdateText.format(formattedTime),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Кнопка поделиться
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val unitStr = if (unit == "C") celsiusText else fahrenheitText
                IconButton(
                    onClick = {
                        val desc = weather.weather.firstOrNull()?.description?.capitalize() ?: ""
                        val temp = weather.main.temp
                        val city = weather.name
                        val shareText = city + ": " + temp + unitStr + ", " + desc
                        onShareWeather(shareText)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = shareWeatherText,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Основная информация о погоде
            Text(
                text = weather.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            weather.weather.firstOrNull()?.let { weatherInfo ->
                WeatherIcon(
                    iconCode = weatherInfo.icon,
                    modifier = Modifier.size(80.dp),
                    contentDescription = weatherInfo.description
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${weather.main.temp}°$unit",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = feelsLikeText.format(weather.main.feels_like, unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = minMaxText.format(weather.main.temp_min, weather.main.temp_max, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = weather.weather.firstOrNull()?.description?.capitalize() ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Детальная информация
            WeatherDetailsGrid(
                weather = weather,
                humidityText = humidityText,
                windSpeedText = windSpeedText,
                pressureText = pressureText,
                cloudinessText = cloudinessText,
                visibilityText = visibilityText
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Время восхода и заката
            SunriseSunsetRow(
                weather = weather,
                sunriseText = sunriseText,
                sunsetText = sunsetText
            )
        }
    }
}

@Composable
fun WeatherDetailsGrid(
    weather: com.example.cf.domain.model.WeatherResponse,
    humidityText: String,
    windSpeedText: String,
    pressureText: String,
    cloudinessText: String,
    visibilityText: String
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            WeatherInfoItem(
                icon = Icons.Default.WaterDrop,
                value = humidityText.format(weather.main.humidity)
            )
        }
        item {
            WeatherInfoItem(
                icon = Icons.Default.Air,
                value = windSpeedText.format(weather.wind.speed)
            )
        }
        item {
            WeatherInfoItem(
                icon = Icons.Default.Speed,
                value = pressureText.format(weather.main.pressure)
            )
        }
        item {
            WeatherInfoItem(
                icon = Icons.Default.Cloud,
                value = cloudinessText.format(weather.clouds.all)
            )
        }
        item {
            WeatherInfoItem(
                icon = Icons.Default.Visibility,
                value = visibilityText.format(weather.visibility / 1000.0)
            )
        }
    }
}

@Composable
fun SunriseSunsetRow(
    weather: com.example.cf.domain.model.WeatherResponse,
    sunriseText: String,
    sunsetText: String
) {
    val sunrise = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(weather.sys.sunrise * 1000))
    val sunset = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(weather.sys.sunset * 1000))
    
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = sunriseText.format(sunrise), 
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = sunsetText.format(sunset), 
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ForecastSection(
    dailyForecasts: List<ForecastItem>,
    unit: String,
    forecastTitleText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = forecastTitleText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(dailyForecasts) { item ->
                ForecastCard(forecast = item, unit = unit)
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ForecastCard(
    forecast: ForecastItem,
    unit: String
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100) // Небольшая задержка для эффекта каскада
        visible = true
    }
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .graphicsLayer {
                alpha = if (visible) 1f else 0f
                translationX = if (visible) 0f else 30f
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = forecast.weather.firstOrNull()?.main ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            forecast.weather.firstOrNull()?.let { weather ->
                WeatherIcon(
                    iconCode = weather.icon,
                    modifier = Modifier.size(40.dp),
                    contentDescription = weather.description
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${forecast.main.temp}°$unit",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    IconButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                shadowElevation = if (isPressed) 2f else 4f
            },
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        icon()
    }
}

@Composable
fun AnimatedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholderText: String,
    clearText: String,
    historyCities: List<String>,
    onHistoryItemClick: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    val scaleX by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { 
                onValueChange(it)
                showHistory = it.isEmpty() && historyCities.isNotEmpty()
            },
            modifier = Modifier
                .scale(scaleX)
                .graphicsLayer {
                    shadowElevation = if (isFocused) 8f else 2f
                }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    showHistory = focusState.isFocused && value.isEmpty() && historyCities.isNotEmpty()
                },
            enabled = enabled,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = clearText,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { 
                    onSearch()
                    showHistory = false
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        // Выпадающий список истории
        AnimatedVisibility(
            visible = showHistory && historyCities.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Недавние города:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    historyCities.take(5).forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onHistoryItemClick(city)
                                    showHistory = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
} 