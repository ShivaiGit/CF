package com.example.cf.core

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

object ErrorHandler {
    
    // Кэш для сообщений об ошибках для улучшения производительности
    private val errorMessageCache = ConcurrentHashMap<String, String>()
    
    // Кэш для классификации ошибок
    private val errorTypeCache = ConcurrentHashMap<Class<*>, ErrorType>()
    
    init {
        // Предварительно заполняем кэш для часто встречающихся ошибок
        errorTypeCache[IOException::class.java] = ErrorType.NETWORK
        errorTypeCache[SocketTimeoutException::class.java] = ErrorType.NETWORK
        errorTypeCache[UnknownHostException::class.java] = ErrorType.NETWORK
        errorTypeCache[HttpException::class.java] = ErrorType.HTTP
    }
    
    fun handleException(exception: Exception): String {
        val exceptionClass = exception::class.java
        val cacheKey = "${exceptionClass.simpleName}_${exception.message}"
        
        // Проверяем кэш
        errorMessageCache[cacheKey]?.let { return it }
        
        val message = when (exception) {
            is IOException -> "Нет подключения к интернету. Проверьте сеть."
            is SocketTimeoutException -> "Превышено время ожидания. Попробуйте позже."
            is UnknownHostException -> "Не удается подключиться к серверу. Проверьте интернет."
            is HttpException -> handleHttpException(exception)
            else -> exception.message ?: "Неизвестная ошибка. Попробуйте позже."
        }
        
        // Кэшируем результат
        errorMessageCache[cacheKey] = message
        return message
    }
    
    private fun handleHttpException(exception: HttpException): String {
        val code = exception.code()
        val cacheKey = "HTTP_$code"
        
        // Проверяем кэш
        errorMessageCache[cacheKey]?.let { return it }
        
        val message = when (code) {
            400 -> "Неверный запрос. Проверьте параметры."
            401 -> "Ошибка авторизации. Проверьте API ключ."
            403 -> "Доступ запрещен. Проверьте права доступа."
            404 -> "Город не найден. Проверьте название."
            429 -> "Слишком много запросов. Попробуйте позже."
            500 -> "Внутренняя ошибка сервера. Попробуйте позже."
            502 -> "Сервер временно недоступен. Попробуйте позже."
            503 -> "Сервис временно недоступен. Попробуйте позже."
            504 -> "Превышено время ожидания сервера. Попробуйте позже."
            in 400..499 -> "Ошибка клиента: $code"
            in 500..599 -> "Ошибка сервера: $code"
            else -> "Неизвестная HTTP ошибка: $code"
        }
        
        // Кэшируем результат
        errorMessageCache[cacheKey] = message
        return message
    }
    
    fun isNetworkError(exception: Exception): Boolean {
        return getErrorType(exception) == ErrorType.NETWORK
    }
    
    fun isAuthError(exception: Exception): Boolean {
        return exception is HttpException && exception.code() == 401
    }
    
    fun isServerError(exception: Exception): Boolean {
        return exception is HttpException && exception.code() in 500..599
    }
    
    fun isClientError(exception: Exception): Boolean {
        return exception is HttpException && exception.code() in 400..499
    }
    
    fun isRetryableError(exception: Exception): Boolean {
        return when (exception) {
            is SocketTimeoutException -> true
            is IOException -> true
            is UnknownHostException -> true
            is HttpException -> {
                val code = exception.code()
                code in 500..599 || code == 429
            }
            else -> false
        }
    }
    
    fun getRetryDelay(exception: Exception, attempt: Int): Long {
        return when (exception) {
            is HttpException -> {
                when (exception.code()) {
                    429 -> 5000L * attempt // Rate limit - увеличиваем задержку
                    in 500..599 -> 2000L * attempt // Server error
                    else -> 1000L * attempt
                }
            }
            is SocketTimeoutException -> 3000L * attempt
            is IOException, is UnknownHostException -> 2000L * attempt
            else -> 1000L * attempt
        }
    }
    
    private fun getErrorType(exception: Exception): ErrorType {
        val exceptionClass = exception::class.java
        
        // Проверяем кэш
        errorTypeCache[exceptionClass]?.let { return it }
        
        val errorType = when (exception) {
            is IOException, is SocketTimeoutException, is UnknownHostException -> ErrorType.NETWORK
            is HttpException -> ErrorType.HTTP
            else -> ErrorType.UNKNOWN
        }
        
        // Кэшируем результат
        errorTypeCache[exceptionClass] = errorType
        return errorType
    }
    
    fun getErrorSeverity(exception: Exception): ErrorSeverity {
        return when (exception) {
            is HttpException -> {
                when (exception.code()) {
                    401, 403 -> ErrorSeverity.CRITICAL
                    404 -> ErrorSeverity.WARNING
                    429 -> ErrorSeverity.WARNING
                    in 500..599 -> ErrorSeverity.ERROR
                    else -> ErrorSeverity.ERROR
                }
            }
            is IOException, is UnknownHostException -> ErrorSeverity.WARNING
            is SocketTimeoutException -> ErrorSeverity.WARNING
            else -> ErrorSeverity.ERROR
        }
    }
    
    fun shouldShowError(exception: Exception): Boolean {
        return getErrorSeverity(exception) != ErrorSeverity.WARNING
    }
    
    fun clearCache() {
        errorMessageCache.clear()
        errorTypeCache.clear()
    }
    
    fun getCacheSize(): Int {
        return errorMessageCache.size + errorTypeCache.size
    }
}

enum class ErrorType {
    NETWORK,
    HTTP,
    UNKNOWN
}

enum class ErrorSeverity {
    WARNING,
    ERROR,
    CRITICAL
} 