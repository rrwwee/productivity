package org.localhackathon.elsewhere

import org.junit.Assert.*
import org.junit.Test

class NudgeTextTest {
    @Test fun dropsOnlyUnfinishedTrailingPhrase() {
        assertEquals("Water modeled an economy!", NudgeText.completeBody("Water modeled an economy! Built by people, it"))
    }

    @Test fun preservesCompletedInvitation() {
        val copy = "Water modeled an economy. See how it worked!"
        assertEquals(copy, NudgeText.completeBody(copy))
    }

    @Test fun rejectsFragmentRatherThanPublishingIt() {
        assertNull(NudgeText.completeBody("Water flowing through the"))
    }
}
