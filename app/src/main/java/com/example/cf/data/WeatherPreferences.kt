package com.example.cf.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_preferences")

class WeatherPreferences(private val context: Context) {
    
    private val LAST_CITY = stringPreferencesKey("last_city")

    val lastCity: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_CITY] ?: ""
        }

    suspend fun saveCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_CITY] = city
        }
    }
} 