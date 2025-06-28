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
    onSettingsClick: () -> Unit = {}
) {
    Log.d("WeatherScreen", "Composing WeatherScreen")
    
    // Собираем все состояния в одном месте для оптимизации
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isCacheShown by viewModel.isCacheShown.collectAsStateWithLifecycle()
    val cacheTimestamp by viewModel.cacheTimestamp.collectAsStateWithLifecycle()
    val historyCities by viewModel.historyCities.collectAsStateWithLifecycle()
    
    Log.d("WeatherScreen", "States collected: city=${state.city}, isLoading=${state.isLoading}, weather=${state.weather != null}")
    
    // Оптимизируем вычисления с помощью derivedStateOf
    val unit by remember { derivedStateOf { if (state.isCelsius) "C" else "F" } }
    
    // Получаем строки ресурсов в начале функции - оптимизация
    val stringResources = remember {
        StringResources(
            myLocation = stringResource(R.string.my_location),
            settings = stringResource(R.string.settings),
            outdatedData = stringResource(R.string.outdated_data),
            lastUpdate = stringResource(R.string.last_update),
            feelsLike = stringResource(R.string.feels_like),
            minMax = stringResource(R.string.min_max),
            humidity = stringResource(R.string.humidity),
            windSpeed = stringResource(R.string.wind_speed),
            pressure = stringResource(R.string.pressure),
            cloudiness = stringResource(R.string.cloudiness),
            visibility = stringResource(R.string.visibility),
            sunrise = stringResource(R.string.sunrise),
            sunset = stringResource(R.string.sunset),
            forecastTitle = stringResource(R.string.forecast_title),
            placeholder = stringResource(R.string.enter_city_name),
            clear = stringResource(R.string.clear)
        )
    }

    // Pull-to-refresh состояние
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = {
            if (state.city.isNotBlank()) {
                viewModel.fetchWeather()
            }
        }
    )

    // Оптимизируем remember для dailyForecasts с ключами
    val dailyForecasts by remember(state.forecast?.list) {
        derivedStateOf {
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
            item(key = "error") {
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
            item(key = "search_panel") {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Поле поиска города с историей
                        AnimatedSearchField(
                            value = state.city,
                            onValueChange = viewModel::onCityChange,
                            onSearch = viewModel::fetchWeather,
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading,
                            placeholderText = stringResources.placeholder,
                            clearText = stringResources.clear,
                            historyCities = historyCities,
                            onHistoryItemClick = { viewModel.selectCityFromHistory(it) }
                        )
                        
                        // Кнопка "Моё местоположение"
                        AnimatedIconButton(
                            onClick = { viewModel.onMyLocationClick() },
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = stringResources.myLocation,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        )
                    }
                }
            }
            
            // Показываем загрузку только если действительно загружаемся и город не пустой
            item(key = "loading") {
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
            item(key = "weather_card") {
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
                            stringResources = stringResources
                        )
                    }
                }
            }
            
            // Показываем прогноз только если он есть
            item(key = "forecast_section") {
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
                            forecastTitleText = stringResources.forecastTitle
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
        
        // Кнопка настроек в правом нижнем углу
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResources.settings,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    
    Log.d("WeatherScreen", "WeatherScreen composition completed")
}

// Класс для хранения строковых ресурсов - оптимизация
private data class StringResources(
    val myLocation: String,
    val settings: String,
    val outdatedData: String,
    val lastUpdate: String,
    val feelsLike: String,
    val minMax: String,
    val humidity: String,
    val windSpeed: String,
    val pressure: String,
    val cloudiness: String,
    val visibility: String,
    val sunrise: String,
    val sunset: String,
    val forecastTitle: String,
    val placeholder: String,
    val clear: String
)

@Composable
fun WeatherCard(
    weather: Weather,
    unit: String,
    isCacheShown: Boolean,
    cacheTimestamp: Long?,
    stringResources: StringResources,
    modifier: Modifier = Modifier
) {
    // Оптимизируем вычисления с помощью remember
    val temperature by remember(weather.main.temp) { 
        derivedStateOf { "${weather.main.temp.toInt()}°$unit" }
    }
    
    val feelsLike by remember(weather.main.feelsLike) { 
        derivedStateOf { "${weather.main.feelsLike.toInt()}°$unit" }
    }
    
    val minMax by remember(weather.main.tempMin, weather.main.tempMax) { 
        derivedStateOf { 
            "${weather.main.tempMin.toInt()}°$unit / ${weather.main.tempMax.toInt()}°$unit" 
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Заголовок с названием города и кэш-индикатором
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weather.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                if (isCacheShown) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResources.outdatedData,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Основная информация о погоде
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Температура и описание
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = temperature,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = weather.weather.firstOrNull()?.description?.capitalize() ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Иконка погоды
                WeatherIcon(
                    weatherCode = weather.weather.firstOrNull()?.id ?: 800,
                    modifier = Modifier.size(80.dp)
                )
            }
            
            // Дополнительная информация
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherInfoRow(
                    icon = Icons.Default.Thermostat,
                    label = stringResources.feelsLike,
                    value = feelsLike
                )
                
                WeatherInfoRow(
                    icon = Icons.Default.ShowChart,
                    label = stringResources.minMax,
                    value = minMax
                )
                
                WeatherInfoRow(
                    icon = Icons.Default.Opacity,
                    label = stringResources.humidity,
                    value = "${weather.main.humidity}%"
                )
                
                WeatherInfoRow(
                    icon = Icons.Default.Air,
                    label = stringResources.windSpeed,
                    value = "${weather.wind.speed} m/s"
                )
                
                WeatherInfoRow(
                    icon = Icons.Default.Speed,
                    label = stringResources.pressure,
                    value = "${weather.main.pressure} hPa"
                )
                
                WeatherInfoRow(
                    icon = Icons.Default.Cloud,
                    label = stringResources.cloudiness,
                    value = "${weather.clouds.all}%"
                )
                
                if (weather.visibility > 0) {
                    WeatherInfoRow(
                        icon = Icons.Default.Visibility,
                        label = stringResources.visibility,
                        value = "${weather.visibility / 1000} km"
                    )
                }
                
                // Время восхода и заката
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeatherInfoRow(
                        icon = Icons.Default.WbSunny,
                        label = stringResources.sunrise,
                        value = formatTime(weather.sys.sunrise),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    WeatherInfoRow(
                        icon = Icons.Default.NightsStay,
                        label = stringResources.sunset,
                        value = formatTime(weather.sys.sunset),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Время последнего обновления
            if (cacheTimestamp != null) {
                Text(
                    text = "${stringResources.lastUpdate}: ${formatDateTime(cacheTimestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WeatherInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

@Composable
fun ForecastSection(
    dailyForecasts: List<ForecastItem>,
    unit: String,
    forecastTitleText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = forecastTitleText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = dailyForecasts,
                    key = { it.dt }
                ) { forecast ->
                    ForecastCard(
                        forecast = forecast,
                        unit = unit
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastCard(
    forecast: ForecastItem,
    unit: String,
    modifier: Modifier = Modifier
) {
    // Оптимизируем вычисления
    val temperature by remember(forecast.main.temp) { 
        derivedStateOf { "${forecast.main.temp.toInt()}°$unit" }
    }
    
    val date by remember(forecast.dt) { 
        derivedStateOf { formatDate(forecast.dt) }
    }

    Card(
        modifier = modifier
            .width(120.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            WeatherIcon(
                weatherCode = forecast.weather.firstOrNull()?.id ?: 800,
                modifier = Modifier.size(40.dp)
            )
            
            Text(
                text = temperature,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            
            Text(
                text = forecast.weather.firstOrNull()?.description?.capitalize() ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
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
    var isExpanded by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .onFocusChanged { focusState ->
                    showHistory = focusState.isFocused && historyCities.isNotEmpty()
                },
            enabled = enabled,
            placeholder = {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            interactionSource = interactionSource,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        // История поиска
        AnimatedVisibility(
            visible = showHistory,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    historyCities.take(5).forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onHistoryItemClick(city)
                                    showHistory = false
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource
    ) {
        icon()
    }
}

// Утилитарные функции
private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val formatter = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    return formatter.format(date)
}

private fun formatDateTime(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

private fun String.capitalize(): String {
    return if (isNotEmpty()) {
        this[0].uppercase() + substring(1)
    } else {
        this
    }
} 