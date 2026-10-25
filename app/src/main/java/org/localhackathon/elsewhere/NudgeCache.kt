package org.localhackathon.elsewhere

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/** Stores only model-written public article invitations, never screen captures. */
object NudgeCache {
    data class Entry(val copy: NudgeCopy, val generationMs: Long, val generatedAtMillis: Long)

    private fun key(article: PreparedArticle, promptIdentity: String): String {
        val input = JSONObject().apply {
            put("id", article.id)
            put("url", article.url)
            put("article", article.promptData())
            put("prompt", promptIdentity)
        }.toString()
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun valid(copy: NudgeCopy): Boolean =
        copy.title.length in 8..65 && copy.body.length in 15..160 &&
            !copy.title.contains("http", ignoreCase = true) &&
            !copy.body.contains("http", ignoreCase = true) &&
            !copy.title.contains('\n') && !copy.body.contains('\n')

    fun read(context: Context, article: PreparedArticle, promptIdentity: String): Entry? = runCatching {
        val raw = context.getSharedPreferences("nudge-copy-cache", Context.MODE_PRIVATE)
            .getString(key(article, promptIdentity), null) ?: return null
        val json = JSONObject(raw)
        val copy = NudgeCopy(json.getString("title"), json.getString("body"))
        require(valid(copy))
        val generationMs = json.getLong("generationMs")
        val generatedAtMillis = json.getLong("generatedAtMillis")
        require(generationMs >= 0 && generatedAtMillis > 0)
        Entry(copy, generationMs, generatedAtMillis)
    }.getOrNull()

    fun write(context: Context, article: PreparedArticle, promptIdentity: String, entry: Entry) {
        require(valid(entry.copy))
        val json = JSONObject().apply {
            put("title", entry.copy.title)
            put("body", entry.copy.body)
            put("generationMs", entry.generationMs)
            put("generatedAtMillis", entry.generatedAtMillis)
        }
        check(context.getSharedPreferences("nudge-copy-cache", Context.MODE_PRIVATE).edit()
            .putString(key(article, promptIdentity), json.toString()).commit()) { "Could not save invitation cache." }
    }
}
