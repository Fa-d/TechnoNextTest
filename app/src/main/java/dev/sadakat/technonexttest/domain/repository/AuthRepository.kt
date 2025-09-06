package dev.sadakat.technonexttest.domain.repository

import dev.sadakat.technonexttest.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun register (email: String, password: String): Result<Unit>
    suspend fun login (email: String, password: String): Result<User>
    suspend fun logout()
    fun getCurrentUser(): Flow<User?>
    suspend fun isUserLoggedIn(): Boolean

    suspend fun userExists(email: String): Boolean


}