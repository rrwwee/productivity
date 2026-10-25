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

    @Test fun doesNotTurnADecimalIntoASentence() {
        assertNull(NudgeText.completeBody("This device costs 399.99 dollars"))
        assertEquals("This device costs 399.99 dollars.", NudgeText.completeBody("This device costs 399.99 dollars."))
    }

    @Test fun abbreviationsDoNotHideAnIncompleteSentence() {
        assertNull(NudgeText.completeBody("The team worked with Dr. Smith on"))
        assertNull(NudgeText.completeBody("Researchers in the U.S. were working on"))
        assertEquals("Dr. Smith explains the machine.", NudgeText.completeBody("Dr. Smith explains the machine."))
    }
}
