package dev.sadakat.technonexttest.util

import org.junit.Assert.*
import org.junit.Test

class PasswordUtilsTest {

    @Test
    fun `valid email returns true`() {
        assertTrue(PasswordUtils.isValidEmail("test@example.com"))
        assertTrue(PasswordUtils.isValidEmail("user.name@domain.co.uk"))
        assertTrue(PasswordUtils.isValidEmail("test+tag@example.org"))
    }
    
    @Test
    fun `invalid email returns false`() {
        assertFalse(PasswordUtils.isValidEmail("invalid"))
        assertFalse(PasswordUtils.isValidEmail("test@"))
        assertFalse(PasswordUtils.isValidEmail("@domain.com"))
        assertFalse(PasswordUtils.isValidEmail("test@domain"))
    }
    
    @Test
    fun `valid password returns true`() {
        assertTrue(PasswordUtils.isValidPassword("Password123!"))
        assertTrue(PasswordUtils.isValidPassword("MyP@ssw0rd"))
        assertTrue(PasswordUtils.isValidPassword("Test123#"))
    }
    
    @Test
    fun `invalid password returns false`() {
        // Too short
        assertFalse(PasswordUtils.isValidPassword("Pw1!"))
        // No uppercase
        assertFalse(PasswordUtils.isValidPassword("password123!"))
        // No lowercase
        assertFalse(PasswordUtils.isValidPassword("PASSWORD123!"))
        // No digit
        assertFalse(PasswordUtils.isValidPassword("Password!"))
        // No special character
        assertFalse(PasswordUtils.isValidPassword("Password123"))
    }
    
    @Test
    fun `password hashing and verification works`() {
        val password = "TestPassword123!"
        val hashedPassword = PasswordUtils.hashPassword(password)
        
        assertTrue(PasswordUtils.verifyPassword(password, hashedPassword))
        assertFalse(PasswordUtils.verifyPassword("WrongPassword", hashedPassword))
    }
    
    @Test
    fun `same password generates different hashes due to salt`() {
        val password = "TestPassword123!"
        val hash1 = PasswordUtils.hashPassword(password)
        val hash2 = PasswordUtils.hashPassword(password)
        
        assertNotEquals(hash1, hash2)
        assertTrue(PasswordUtils.verifyPassword(password, hash1))
        assertTrue(PasswordUtils.verifyPassword(password, hash2))
    }
}