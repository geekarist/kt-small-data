package me.cpele.smalldata.core

interface Obsidian {
    fun findNotes(query: String): List<Finding>

    data class Finding(val label: String)

    data class Details(val authenticated: Boolean, val status: String, val versions: Versions) {
        data class Versions(val obsidian: String, val restApi: String)
    }
}
