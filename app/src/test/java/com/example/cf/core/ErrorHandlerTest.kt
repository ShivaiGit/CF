package com.example.cf.core

import org.junit.Test
import org.junit.Assert.*
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class ErrorHandlerTest {

    @Test
    fun `handleException returns correct message for IOException`() {
        val exception = IOException("Network error")
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Нет подключения к интернету. Проверьте сеть.", message)
    }

    @Test
    fun `handleException returns correct message for SocketTimeoutException`() {
        val exception = SocketTimeoutException("Timeout")
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Превышено время ожидания. Попробуйте позже.", message)
    }

    @Test
    fun `handleException returns correct message for UnknownHostException`() {
        val exception = UnknownHostException("Unknown host")
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Не удается подключиться к серверу. Проверьте интернет.", message)
    }

    @Test
    fun `handleException returns correct message for 401 HttpException`() {
        val responseBody = "Unauthorized".toResponseBody("text/plain".toMediaType())
        val exception = HttpException(Response.error<Any>(401, responseBody))
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Ошибка авторизации. Проверьте API ключ.", message)
    }

    @Test
    fun `handleException returns correct message for 404 HttpException`() {
        val responseBody = "Not Found".toResponseBody("text/plain".toMediaType())
        val exception = HttpException(Response.error<Any>(404, responseBody))
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Город не найден. Проверьте название.", message)
    }

    @Test
    fun `handleException returns correct message for 429 HttpException`() {
        val responseBody = "Too Many Requests".toResponseBody("text/plain".toMediaType())
        val exception = HttpException(Response.error<Any>(429, responseBody))
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Слишком много запросов. Попробуйте позже.", message)
    }

    @Test
    fun `handleException returns correct message for 500 HttpException`() {
        val responseBody = "Internal Server Error".toResponseBody("text/plain".toMediaType())
        val exception = HttpException(Response.error<Any>(500, responseBody))
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Ошибка сервера. Попробуйте позже.", message)
    }

    @Test
    fun `handleException returns correct message for unknown HttpException`() {
        val responseBody = "Unknown Error".toResponseBody("text/plain".toMediaType())
        val exception = HttpException(Response.error<Any>(999, responseBody))
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Ошибка сервера: 999", message)
    }

    @Test
    fun `handleException returns fallback message for unknown exception`() {
        val exception = RuntimeException("Unknown error")
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Unknown error", message)
    }

    @Test
    fun `handleException returns default message for exception without message`() {
        val exception = RuntimeException()
        val message = ErrorHandler.handleException(exception)
        
        assertEquals("Неизвестная ошибка. Попробуйте позже.", message)
    }

    @Test
    fun `isNetworkError returns true for network exceptions`() {
        assertTrue(ErrorHandler.isNetworkError(IOException()))
        assertTrue(ErrorHandler.isNetworkError(SocketTimeoutException()))
        assertTrue(ErrorHandler.isNetworkError(UnknownHostException()))
    }

    @Test
    fun `isNetworkError returns false for non-network exceptions`() {
        assertFalse(ErrorHandler.isNetworkError(RuntimeException()))
        assertFalse(ErrorHandler.isNetworkError(IllegalArgumentException()))
    }

    @Test
    fun `isAuthError returns true for 401 HttpException`() {
        val responseBody = "Unauthorized".toResponseBody("text/plain".toMediaType())
        val exception = HttpException(Response.error<Any>(401, responseBody))
        
        assertTrue(ErrorHandler.isAuthError(exception))
    }

    @Test
    fun `isAuthError returns false for other exceptions`() {
        assertFalse(ErrorHandler.isAuthError(IOException()))
        assertFalse(ErrorHandler.isAuthError(RuntimeException()))
    }

    @Test
    fun `isServerError returns true for 5xx HttpException`() {
        val responseBody = "Server Error".toResponseBody("text/plain".toMediaType())
        val exception500 = HttpException(Response.error<Any>(500, responseBody))
        val exception502 = HttpException(Response.error<Any>(502, responseBody))
        val exception503 = HttpException(Response.error<Any>(503, responseBody))
        
        assertTrue(ErrorHandler.isServerError(exception500))
        assertTrue(ErrorHandler.isServerError(exception502))
        assertTrue(ErrorHandler.isServerError(exception503))
    }

    @Test
    fun `isServerError returns false for non-5xx exceptions`() {
        assertFalse(ErrorHandler.isServerError(IOException()))
        assertFalse(ErrorHandler.isServerError(RuntimeException()))
    }
} 