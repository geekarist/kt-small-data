package me.cpele.smalldata.core

interface GoogleEffects {
    suspend fun search(query: String): List<Finding>

    data class Finding(val label: String)
}
