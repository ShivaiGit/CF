package com.example.cf.ui.weather

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.CachePolicy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun WeatherIcon(
    weatherCode: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    // Оптимизируем создание URL с помощью remember
    val iconUrl by remember(weatherCode) {
        derivedStateOf {
            val iconCode = when (weatherCode) {
                // Грозы
                in 200..232 -> "11d"
                // Морось
                in 300..321 -> "09d"
                // Дождь
                in 500..531 -> "10d"
                // Снег
                in 600..622 -> "13d"
                // Туман
                in 701..781 -> "50d"
                // Ясно
                800 -> "01d"
                // Облачно
                in 801..804 -> "02d"
                else -> "01d"
            }
            "https://openweathermap.org/img/wn/$iconCode@2x.png"
        }
    }
    
    AsyncImage(
        model = iconUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        // Оптимизация кэширования
        onLoading = { placeholder ->
            // Можно добавить placeholder если нужно
        },
        onSuccess = { state ->
            // Успешная загрузка
        },
        onError = { state ->
            // Обработка ошибки загрузки
        }
    )
}

// Альтернативная версия с иконками Material Design для лучшей производительности
@Composable
fun WeatherIconMaterial(
    weatherCode: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val icon by remember(weatherCode) {
        derivedStateOf {
            when (weatherCode) {
                // Грозы
                in 200..232 -> Icons.Default.Thunderstorm
                // Морось
                in 300..321 -> Icons.Default.Grain
                // Дождь
                in 500..531 -> Icons.Default.Opacity
                // Снег
                in 600..622 -> Icons.Default.AcUnit
                // Туман
                in 701..781 -> Icons.Default.Cloud
                // Ясно
                800 -> Icons.Default.WbSunny
                // Облачно
                in 801..804 -> Icons.Default.Cloud
                else -> Icons.Default.WbSunny
            }
        }
    }
    
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
    )
} 