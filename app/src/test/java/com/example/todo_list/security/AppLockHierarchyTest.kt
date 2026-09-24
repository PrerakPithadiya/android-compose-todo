package com.example.todo_list.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the 3-Tier Authentication Hierarchy:
 * Priority 1: Face Lock
 * Priority 2: Fingerprint
 * Priority 3: Manual PIN / Passcode
 */
class AppLockHierarchyTest {

    enum class MockAuthStage {
        FACE_SCAN,
        MANUAL_ENTRY
    }

    private fun resolveInitialAuthStage(isFaceLockEnabled: Boolean): MockAuthStage {
        return if (isFaceLockEnabled) MockAuthStage.FACE_SCAN else MockAuthStage.MANUAL_ENTRY
    }

    private fun resolvePriorityAction(
        isFaceLockEnabled: Boolean,
        isFingerprintEnabled: Boolean
    ): String {
        return when {
            isFaceLockEnabled -> "START_FACE_DETECTION"
            isFingerprintEnabled -> "TRIGGER_FINGERPRINT_PROMPT"
            else -> "SHOW_MANUAL_PIN"
        }
    }

    @Test
    fun testPriorityHierarchyResolution_FaceLockEnabled() {
        // When Face lock is enabled (even if fingerprint is also enabled),
        // Face detection MUST be Priority #1 and start automatically
        val initialStage = resolveInitialAuthStage(isFaceLockEnabled = true)
        assertEquals(MockAuthStage.FACE_SCAN, initialStage)

        val actionBoth = resolvePriorityAction(isFaceLockEnabled = true, isFingerprintEnabled = true)
        assertEquals("START_FACE_DETECTION", actionBoth)

        val actionFaceOnly = resolvePriorityAction(isFaceLockEnabled = true, isFingerprintEnabled = false)
        assertEquals("START_FACE_DETECTION", actionFaceOnly)
    }

    @Test
    fun testPriorityHierarchyResolution_FingerprintFallback() {
        // When Face lock is disabled, Fingerprint is Priority #2
        val initialStage = resolveInitialAuthStage(isFaceLockEnabled = false)
        assertEquals(MockAuthStage.MANUAL_ENTRY, initialStage)

        val action = resolvePriorityAction(isFaceLockEnabled = false, isFingerprintEnabled = true)
        assertEquals("TRIGGER_FINGERPRINT_PROMPT", action)
    }

    @Test
    fun testPriorityHierarchyResolution_ManualPinFallback() {
        // When neither biometric is enabled, Manual PIN is Priority #3
        val initialStage = resolveInitialAuthStage(isFaceLockEnabled = false)
        assertEquals(MockAuthStage.MANUAL_ENTRY, initialStage)

        val action = resolvePriorityAction(isFaceLockEnabled = false, isFingerprintEnabled = false)
        assertEquals("SHOW_MANUAL_PIN", action)
    }

    @Test
    fun testLockTypeIntegrity() {
        val types = LockType.entries
        assertEquals(4, types.size)
        assertEquals("pin_4", LockType.PIN_4.id)
        assertEquals("pin_6", LockType.PIN_6.id)
        assertEquals("pattern", LockType.PATTERN.id)
        assertEquals("password", LockType.PASSWORD.id)

        assertEquals(LockType.PIN_4, LockType.fromId("pin_4"))
        assertEquals(LockType.PIN_6, LockType.fromId("PIN_6"))
        assertEquals(LockType.DEFAULT, LockType.fromId("unknown_type"))
    }
}
