package com.example.todo_list.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthManagerTest {

    @Test
    fun testOtpGenerationAndVerification() {
        val testPhone = "+1234567890"
        val code = AuthManager.sendOtp(testPhone)

        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
        assertEquals(testPhone, AuthManager.pendingPhone)
        assertFalse(AuthManager.isOtpExpired())

        // Verify correct OTP
        assertTrue(AuthManager.verifyOtp(code))

        // Verify incorrect OTP
        assertFalse(AuthManager.verifyOtp("000000"))

        // Expire OTP
        AuthManager.expireCurrentOtp()
        assertTrue(AuthManager.isOtpExpired())
        assertFalse(AuthManager.verifyOtp(code))
    }

    @Test
    fun testClearPendingOtp() {
        AuthManager.sendOtp("+9876543210")
        assertFalse(AuthManager.isOtpExpired())

        AuthManager.clearPendingOtp()
        assertTrue(AuthManager.isOtpExpired())
    }

    @Test
    fun testPasswordValidationWithoutActiveUser() {
        // When no active user is set, validateCurrentPassword returns true as a non-blocking fallback
        assertTrue(AuthManager.validateCurrentPassword("any_password"))
        assertFalse(AuthManager.isCurrentPasswordSet())
    }

    @Test
    fun testOtpValidityWindowIs30Seconds() {
        assertEquals(30, AuthManager.OTP_VALIDITY_SECONDS)
        val testPhone = "+919876543210"
        AuthManager.sendOtp(testPhone)

        val expectedExpiry = AuthManager.otpSentTimestamp + (30 * 1000L)
        assertEquals(expectedExpiry, AuthManager.otpExpiryTimestamp)
        assertFalse(AuthManager.isOtpExpired())
    }
}
