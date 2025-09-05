package dev.sadakat.technonexttest.data.respository

import dev.sadakat.technonexttest.data.local.database.dao.UserDao
import dev.sadakat.technonexttest.data.local.database.entities.UserEntity
import dev.sadakat.technonexttest.data.local.preferences.UserPreferences
import dev.sadakat.technonexttest.domain.model.User
import dev.sadakat.technonexttest.domain.repository.AuthRepository
import dev.sadakat.technonexttest.util.PasswordUtils
import dev.sadakat.technonexttest.util.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao, private val userPreferences: UserPreferences
) : AuthRepository {
    override suspend fun register(
        email: String, password: String
    ): Result<Unit> {
        return try {
            if (!PasswordUtils.isValidEmail(email)) {
                return Result.failure(Exception("Invalid email format"))
            }
            if (!PasswordUtils.isValidPassword(password)) {
                return Result.failure(Exception("Invalid password format"))
            }
            val hashedPassword = PasswordUtils.hashPassword(password)
            val userEntity = UserEntity(email, hashedPassword)
            userDao.insertUser(userEntity)
            Result.success(Unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(
        email: String, password: String
    ): Result<User> {
        return try {
            val userEntity =
                userDao.getUserByEmail(email) ?: return Result.failure(Exception("User not found"))

            if (PasswordUtils.verifyPassword(password, userEntity.passwordHash)) {
                userPreferences.setLoggedInUser(email)
                Result.success(userEntity.toDomain())
            } else {
                Result.failure(Exception("Invalid password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        userPreferences.clearLoggedInUser()
    }

    override fun getCurrentUser(): Flow<User?> {
        return userPreferences.getLoggedInUser().map { email ->
            email?.let {
                userDao.getUserByEmail(it)?.toDomain()
            }
        }
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return userPreferences.getLoggedInUserSync() != null
    }


}