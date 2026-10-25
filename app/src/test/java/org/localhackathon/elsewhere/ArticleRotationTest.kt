package org.localhackathon.elsewhere

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleRotationTest {
    private val articles = listOf("brain", "microduck", "attention")

    @Test fun visitsEveryArticleBeforeRepeating() {
        var last: String? = null
        val delivered = (1..6).map {
            ArticleRotation.nextId(articles, last).also { last = it }
        }
        assertEquals(articles + articles, delivered)
    }

    @Test fun canceledAttemptDoesNotAdvanceTheLastDeliveredCursor() {
        val lastDelivered = "brain"
        assertEquals("microduck", ArticleRotation.nextId(articles, lastDelivered))
        assertEquals("microduck", ArticleRotation.nextId(articles, lastDelivered))
    }

    @Test fun removedArticleFallsBackToFirstRemainingArticle() {
        assertEquals("brain", ArticleRotation.nextId(articles, "removed"))
    }

    @Test fun singleArticleRemainsUsable() {
        assertEquals("only", ArticleRotation.nextId(listOf("only"), "only"))
    }
}
