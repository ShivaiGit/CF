package com.example.cf.core

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
    object Idle : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading
    fun isIdle(): Boolean = this is Idle
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    fun exceptionOrNull(): Exception? = when (this) {
        is Error -> exception
        else -> null
    }
} 