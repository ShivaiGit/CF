package com.example.cf.core

import org.junit.Test
import org.junit.Assert.*

class ResultTest {

    @Test
    fun `Success isSuccess returns true`() {
        val result = Result.Success("test")
        assertTrue(result.isSuccess())
    }

    @Test
    fun `Success isError returns false`() {
        val result = Result.Success("test")
        assertFalse(result.isError())
    }

    @Test
    fun `Success isLoading returns false`() {
        val result = Result.Success("test")
        assertFalse(result.isLoading())
    }

    @Test
    fun `Success getOrNull returns data`() {
        val data = "test data"
        val result = Result.Success(data)
        assertEquals(data, result.getOrNull())
    }

    @Test
    fun `Success exceptionOrNull returns null`() {
        val result = Result.Success("test")
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `Error isSuccess returns false`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)
        assertFalse(result.isSuccess())
    }

    @Test
    fun `Error isError returns true`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)
        assertTrue(result.isError())
    }

    @Test
    fun `Error isLoading returns false`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)
        assertFalse(result.isLoading())
    }

    @Test
    fun `Error getOrNull returns null`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)
        assertNull(result.getOrNull())
    }

    @Test
    fun `Error exceptionOrNull returns exception`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `Loading isSuccess returns false`() {
        val result = Result.Loading
        assertFalse(result.isSuccess())
    }

    @Test
    fun `Loading isError returns false`() {
        val result = Result.Loading
        assertFalse(result.isError())
    }

    @Test
    fun `Loading isLoading returns true`() {
        val result = Result.Loading
        assertTrue(result.isLoading())
    }

    @Test
    fun `Loading getOrNull returns null`() {
        val result = Result.Loading
        assertNull(result.getOrNull())
    }

    @Test
    fun `Loading exceptionOrNull returns null`() {
        val result = Result.Loading
        assertNull(result.exceptionOrNull())
    }
} 