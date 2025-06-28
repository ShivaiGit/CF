package com.example.cf.core

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class NetworkBoundResource<T>(
    private val fetchFromDb: () -> Flow<T?>,
    private val shouldFetch: (T?) -> Boolean,
    private val fetchFromNetwork: suspend () -> T,
    private val saveToDb: suspend (T) -> Unit,
    private val onFetchFailed: (Exception) -> Unit = {},
    private val networkTimeout: Long = 10000, // 10 секунд timeout
    private val maxRetries: Int = 2
) {
    
    fun asFlow(): Flow<Result<T>> = flow {
        emit(Result.Loading)
        
        // Сначала пытаемся получить данные из БД
        val dbValue = fetchFromDb().first()
        
        // Если есть данные в БД, показываем их сразу
        dbValue?.let {
            emit(Result.Success(it))
        }
        
        // Проверяем, нужно ли загружать с сети
        if (shouldFetch(dbValue)) {
            emit(Result.Loading)
            
            try {
                // Загружаем с сети с timeout и retry
                val networkValue = fetchWithRetry()
                saveToDb(networkValue)
                emit(Result.Success(networkValue))
            } catch (e: Exception) {
                onFetchFailed(e)
                
                // Если есть кэшированные данные, показываем их
                dbValue?.let {
                    emit(Result.Success(it))
                } ?: emit(Result.Error(e))
            }
        }
    }.catch { e ->
        // Обрабатываем ошибки Flow
        onFetchFailed(e as? Exception ?: Exception(e.message))
        emit(Result.Error(e as? Exception ?: Exception(e.message)))
    }
    
    private suspend fun fetchWithRetry(): T {
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return withTimeout(networkTimeout) {
                    fetchFromNetwork()
                }
            } catch (e: TimeoutCancellationException) {
                lastException = Exception("Network timeout after ${networkTimeout}ms")
                if (attempt < maxRetries) {
                    delay(1000L * (attempt + 1)) // Экспоненциальная задержка
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(1000L * (attempt + 1)) // Экспоненциальная задержка
                }
            }
        }
        
        throw lastException ?: Exception("Unknown error occurred")
    }
}

// Оптимизированная версия для простых случаев
class SimpleNetworkBoundResource<T>(
    private val fetchFromNetwork: suspend () -> T,
    private val saveToDb: suspend (T) -> Unit,
    private val fetchFromDb: () -> Flow<T?>,
    private val shouldFetch: (T?) -> Boolean = { true }
) {
    
    fun asFlow(): Flow<Result<T>> = flow {
        emit(Result.Loading)
        
        val dbValue = fetchFromDb().first()
        
        if (shouldFetch(dbValue)) {
            try {
                val networkValue = fetchFromNetwork()
                saveToDb(networkValue)
                emit(Result.Success(networkValue))
            } catch (e: Exception) {
                dbValue?.let {
                    emit(Result.Success(it))
                } ?: emit(Result.Error(e))
            }
        } else {
            dbValue?.let {
                emit(Result.Success(it))
            }
        }
    }
}

// Кэшированный NetworkBoundResource для часто запрашиваемых данных
class CachedNetworkBoundResource<T>(
    private val fetchFromDb: () -> Flow<T?>,
    private val shouldFetch: (T?) -> Boolean,
    private val fetchFromNetwork: suspend () -> T,
    private val saveToDb: suspend (T) -> Unit,
    private val onFetchFailed: (Exception) -> Unit = {},
    private val cacheTimeout: Long = 300000 // 5 минут
) {
    
    private var lastFetchTime: Long = 0
    private var cachedData: T? = null
    
    fun asFlow(): Flow<Result<T>> = flow {
        emit(Result.Loading)
        
        val currentTime = System.currentTimeMillis()
        val isCacheValid = (currentTime - lastFetchTime) < cacheTimeout
        
        // Проверяем кэш в памяти
        if (isCacheValid && cachedData != null) {
            emit(Result.Success(cachedData!!))
            return@flow
        }
        
        // Проверяем БД
        val dbValue = fetchFromDb().first()
        
        if (shouldFetch(dbValue)) {
            try {
                val networkValue = fetchFromNetwork()
                saveToDb(networkValue)
                
                // Обновляем кэш
                cachedData = networkValue
                lastFetchTime = currentTime
                
                emit(Result.Success(networkValue))
            } catch (e: Exception) {
                onFetchFailed(e)
                dbValue?.let {
                    emit(Result.Success(it))
                } ?: emit(Result.Error(e))
            }
        } else {
            dbValue?.let {
                // Обновляем кэш из БД
                cachedData = it
                lastFetchTime = currentTime
                emit(Result.Success(it))
            }
        }
    }
} 