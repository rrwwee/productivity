package org.localhackathon.elsewhere

import org.junit.Assert.*
import org.junit.Test

class NudgeEpisodeTest {
    @Test fun ignoredInvitationsContinueInSameEpisodeWithBoundedContext() {
        val episode = NudgeEpisode()
        val id = episode.begin()
        repeat(5) { index ->
            assertEquals(id, episode.begin())
            assertTrue(episode.record(id, NudgeCopy("Title $index", "Body $index")))
        }
        assertEquals(5, episode.sentCount)
        assertEquals(listOf("Title 2", "Title 3", "Title 4"), episode.history.map { it.title })
    }

    @Test fun resolutionClearsContextAndRejectsLateCopyFromOldEpisode() {
        val episode = NudgeEpisode()
        val old = episode.begin()
        episode.record(old, NudgeCopy("Old title", "Old body"))
        assertTrue(episode.resolve())
        assertFalse(episode.resolve())
        assertEquals(1, episode.completedCount)
        assertTrue(episode.history.isEmpty())
        assertEquals(0, episode.sentCount)
        val fresh = episode.begin()
        assertNotEquals(old, fresh)
        assertFalse(episode.record(old, NudgeCopy("Stale title", "Stale body")))
        assertTrue(episode.record(fresh, NudgeCopy("New title", "New body")))
    }

    @Test fun manualResetDoesNotCountAsSuccess() {
        val episode = NudgeEpisode()
        val id = episode.begin()
        episode.clear()
        assertEquals(0, episode.completedCount)
        assertFalse(episode.record(id, NudgeCopy("Late title", "Late body")))
    }
}
