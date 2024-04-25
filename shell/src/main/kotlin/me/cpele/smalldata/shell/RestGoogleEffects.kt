package me.cpele.smalldata.shell

import me.cpele.smalldata.core.GoogleEffects

class RestGoogleEffects : GoogleEffects {
    override suspend fun search(query: String): List<GoogleEffects.Findings> {
        TODO("Not yet implemented")
    }
}
