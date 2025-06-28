package com.example.cf.di

import android.content.Context
import com.example.cf.BuildConfig
import com.example.cf.data.WeatherApiService
import com.example.cf.data.WeatherPreferences
import com.example.cf.data.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService {
        return retrofit.create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherPreferences(@ApplicationContext context: Context): WeatherPreferences {
        return WeatherPreferences(context)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(
        apiService: WeatherApiService,
        apiKey: String
    ): WeatherRepository {
        return WeatherRepository(apiKey, apiService)
    }

    @Provides
    @Singleton
    fun provideApiKey(): String {
        return BuildConfig.WEATHER_API_KEY
    }
} 