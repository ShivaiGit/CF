package com.example.cf.ui.weather

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun WeatherIcon(
    iconCode: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
    
    AsyncImage(
        model = iconUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
} 