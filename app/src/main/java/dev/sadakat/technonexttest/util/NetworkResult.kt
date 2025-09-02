package dev.sadakat.technonexttest.util

sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String) : NetworkResult<T>()
    data class Loading<T>(val data: T? = null) : NetworkResult<T>()
}