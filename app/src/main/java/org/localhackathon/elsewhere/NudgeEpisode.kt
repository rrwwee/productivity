package org.localhackathon.elsewhere

/** One attempt to redirect attention; stores only our own public invitations. */
class NudgeEpisode {
    private var sequence = 0L
    var id: Long? = null
        private set
    var sentCount = 0
        private set
    var completedCount = 0
        private set
    private val recent = mutableListOf<NudgeCopy>()
    val history: List<NudgeCopy> get() = recent.toList()

    fun begin(): Long = id ?: (++sequence).also { id = it }

    fun record(expectedId: Long, copy: NudgeCopy): Boolean {
        if (id != expectedId) return false
        sentCount++
        recent.add(copy)
        if (recent.size > 3) recent.removeAt(0)
        return true
    }

    fun resolve(): Boolean {
        if (id == null) return false
        completedCount++
        clear()
        return true
    }

    fun clear() {
        id = null
        sentCount = 0
        recent.clear()
    }
}
