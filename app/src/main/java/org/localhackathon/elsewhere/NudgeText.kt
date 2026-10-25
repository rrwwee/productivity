package org.localhackathon.elsewhere

object NudgeText {
    const val HEADLINE = "STOP. Read this instead."
    /** Constrained decoding can close JSON in the middle of a final sentence. */
    fun completeBody(raw: String): String? {
        val body = raw.replace('\n', ' ').trim()
        val abbreviations = setOf("mr", "mrs", "ms", "dr", "prof", "vs", "etc", "e.g", "i.e")
        val end = body.indices.lastOrNull { index ->
            val character = body[index]
            if (character !in ".!?…") return@lastOrNull false
            if (index + 1 < body.length && !body[index + 1].isWhitespace()) return@lastOrNull false
            if (character == '.') {
                val token = Regex("[A-Za-z.]+$").find(body.substring(0, index))?.value.orEmpty()
                if (token.lowercase() in abbreviations || token.length == 1 || '.' in token) return@lastOrNull false
            }
            true
        } ?: -1
        if (end < 0) return null
        return body.substring(0, end + 1).takeIf { it.length in 15..130 }
    }
}
