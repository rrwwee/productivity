package org.localhackathon.elsewhere

/** Advance only from the last article actually posted, not from attempted generations. */
object ArticleRotation {
    fun nextId(articleIds: List<String>, lastDeliveredId: String?): String {
        require(articleIds.isNotEmpty()) { "No saved articles." }
        val lastIndex = articleIds.indexOf(lastDeliveredId)
        return articleIds[(lastIndex + 1) % articleIds.size]
    }
}
