package dev.sadakat.technonexttest.util

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {

    fun hashPassword(password: String, salt: String = generateSalt()): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = password + salt
        val hashedBytes = digest.digest(saltedPassword.toByteArray())
        return "${
            hashedBytes.joinToString("") { "%02x".format(it) }
        }:$salt"
    }

    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        val parts = hashedPassword.split(":")
        if (parts.size != 2) return false
        val (hash, salt) = parts
        val newHash = hashPassword(password, salt).split(":")[0]
        return newHash == hash
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("[a-zA-Z0-9._%+-]+[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8 && password.any { it.isUpperCase() } && password.any { it.isLowerCase() } && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() }
    }
}