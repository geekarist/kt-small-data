package me.cpele.smalldata.core

interface Obsidian {
    fun findNotes(query: String): List<Finding>

    data class Finding(val label: String)
}
