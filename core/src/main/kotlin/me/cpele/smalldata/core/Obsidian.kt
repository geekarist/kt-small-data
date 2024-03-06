package me.cpele.smalldata.core

interface Obsidian {
    fun notes(query: String): List<Finding>
    suspend fun auth(): Details

    data class Finding(val label: String)

    interface Details {
        val status: String
        val versions: Versions
        val service: String
        val authenticated: Boolean

        interface Versions {
            val obsidian: String
            val self: String
        }
    }
}
