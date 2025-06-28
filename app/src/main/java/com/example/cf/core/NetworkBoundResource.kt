package com.example.cf.core

import kotlinx.coroutines.flow.*

class NetworkBoundResource<T>(
    private val fetchFromDb: () -> Flow<T?>,
    private val shouldFetch: (T?) -> Boolean,
    private val fetchFromNetwork: suspend () -> T,
    private val saveToDb: suspend (T) -> Unit,
    private val onFetchFailed: (Exception) -> Unit = {}
) {
    
    fun asFlow(): Flow<Result<T>> = flow {
        emit(Result.Loading)
        
        val dbValue = fetchFromDb().first()
        
        if (shouldFetch(dbValue)) {
            emit(Result.Loading)
            
            try {
                val networkValue = fetchFromNetwork()
                saveToDb(networkValue)
                emit(Result.Success(networkValue))
            } catch (e: Exception) {
                onFetchFailed(e)
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