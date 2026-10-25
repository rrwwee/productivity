package org.localhackathon.elsewhere

object NudgeText {
    const val HEADLINE = "STOP. Read this instead."
    /** Constrained decoding can close JSON in the middle of a final sentence. */
    fun completeBody(raw: String): String? {
        val body = raw.replace('\n', ' ').trim()
        val end = body.indexOfLast { it == '.' || it == '!' || it == '?' || it == '…' }
        if (end < 0) return null
        return body.substring(0, end + 1).takeIf { it.length in 15..130 }
    }
}
