package com.example.todo_list.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BiometricModalityTest {

    @Test
    fun testBiometricModalityEnumValues() {
        val modalities = BiometricModality.entries
        assertEquals(5, modalities.size)
        assertNotNull(BiometricModality.NONE)
        assertNotNull(BiometricModality.FINGERPRINT_ONLY)
        assertNotNull(BiometricModality.FACE_ONLY)
        assertNotNull(BiometricModality.FACE_AND_FINGERPRINT)
        assertNotNull(BiometricModality.GENERIC_BIOMETRIC)
    }

    @Test
    fun testBiometricAvailabilityEnumValues() {
        val availabilities = BiometricAvailability.entries
        assertEquals(4, availabilities.size)
        assertNotNull(BiometricAvailability.AVAILABLE)
        assertNotNull(BiometricAvailability.NOT_ENROLLED)
        assertNotNull(BiometricAvailability.NO_HARDWARE)
        assertNotNull(BiometricAvailability.HW_UNAVAILABLE)
    }
}
