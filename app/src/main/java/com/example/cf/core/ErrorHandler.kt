package com.example.cf.core

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {
    
    fun handleException(exception: Exception): String {
        return when (exception) {
            is IOException -> "Нет подключения к интернету. Проверьте сеть."
            is SocketTimeoutException -> "Превышено время ожидания. Попробуйте позже."
            is UnknownHostException -> "Не удается подключиться к серверу. Проверьте интернет."
            is HttpException -> handleHttpException(exception)
            else -> exception.message ?: "Неизвестная ошибка. Попробуйте позже."
        }
    }
    
    private fun handleHttpException(exception: HttpException): String {
        return when (exception.code()) {
            401 -> "Ошибка авторизации. Проверьте API ключ."
            404 -> "Город не найден. Проверьте название."
            429 -> "Слишком много запросов. Попробуйте позже."
            500 -> "Ошибка сервера. Попробуйте позже."
            502, 503, 504 -> "Сервер временно недоступен. Попробуйте позже."
            else -> "Ошибка сервера: ${exception.code()}"
        }
    }
    
    fun isNetworkError(exception: Exception): Boolean {
        return exception is IOException || 
               exception is SocketTimeoutException || 
               exception is UnknownHostException
    }
    
    fun isAuthError(exception: Exception): Boolean {
        return exception is HttpException && exception.code() == 401
    }
    
    fun isServerError(exception: Exception): Boolean {
        return exception is HttpException && exception.code() in 500..599
    }
} 