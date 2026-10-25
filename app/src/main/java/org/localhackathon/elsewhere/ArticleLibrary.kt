package org.localhackathon.elsewhere

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PreparedArticle(
    val id: String,
    val title: String,
    val url: String,
    val facts: List<String>,
    val cautions: String,
) {
    fun promptData(): String = JSONObject().apply {
        put("title", title)
        put("facts", JSONArray(facts))
        put("accuracy_notes", cautions)
    }.toString()
}

object ArticleLibrary {
    private var cached: List<PreparedArticle>? = null

    fun all(context: Context): List<PreparedArticle> {
        cached?.let { return it }
        val data = JSONArray(context.assets.open("demo-articles.json").bufferedReader().use { it.readText() })
        return List(data.length()) { index ->
            val row = data.getJSONObject(index)
            val facts = row.getJSONArray("facts")
            PreparedArticle(row.getString("id"), row.getString("title"), row.getString("url"),
                List(facts.length()) { facts.getString(it) }, row.getString("cautions"))
        }.also { cached = it }
    }

    fun selected(context: Context): PreparedArticle {
        val id = context.getSharedPreferences("demo", Context.MODE_PRIVATE).getString("article", null)
        return all(context).firstOrNull { it.id == id } ?: all(context).first()
    }

    fun select(context: Context, id: String) {
        require(all(context).any { it.id == id }) { "Unknown prepared article." }
        context.getSharedPreferences("demo", Context.MODE_PRIVATE).edit().putString("article", id).apply()
    }

    fun next(context: Context): PreparedArticle {
        val articles = all(context)
        // Treat the previously pinned article as the starting cursor on upgrade.
        val last = context.getSharedPreferences("demo", Context.MODE_PRIVATE)
            .getString("last_delivered_article", selected(context).id)
        val nextId = ArticleRotation.nextId(articles.map { it.id }, last)
        return articles.first { it.id == nextId }
    }

    fun recordDelivered(context: Context, article: PreparedArticle) {
        context.getSharedPreferences("demo", Context.MODE_PRIVATE).edit()
            .putString("last_delivered_article", article.id).apply()
    }

    fun rule(context: Context): String =
        context.assets.open("demo-rule.txt").bufferedReader().use { it.readText().trim() }
}
