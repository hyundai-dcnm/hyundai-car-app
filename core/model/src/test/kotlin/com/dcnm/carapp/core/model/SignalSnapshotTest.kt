package com.dcnm.carapp.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalSnapshotTest {
    @Test
    fun `covered length is reported as is`() {
        val snapshot = SignalSnapshot(
            anchorMillis = 10_000,
            requestedBeforeMillis = 30_000,
            requestedAfterMillis = 5_000,
            coveredMillis = 12_000,
            samples = emptyList(),
        )
        assertEquals(12_000, snapshot.coveredMillis)
    }
}
