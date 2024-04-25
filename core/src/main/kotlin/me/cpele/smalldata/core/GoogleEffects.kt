package me.cpele.smalldata.core

interface GoogleEffects {
    suspend fun search(query: String): List<Findings>

    data class Findings(val label: String)
}
