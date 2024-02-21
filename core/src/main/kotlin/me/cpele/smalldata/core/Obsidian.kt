package me.cpele.smalldata.core

interface Obsidian {
    fun notes(query: String): List<Finding>
    fun auth(): Details

    data class Finding(val label: String)

    data class Details(val authenticated: Boolean, val status: String, val versions: Versions) {
        data class Versions(val obsidian: String, val restApi: String)
    }
}
