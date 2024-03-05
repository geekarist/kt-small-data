package me.cpele.smalldata.shell

import me.cpele.smalldata.core.Obsidian

object FakeObsidian : Obsidian {
    override fun notes(query: String): List<Obsidian.Finding> = (1..10).map { num ->
        Obsidian.Finding("Note n°$num: $query")
    }

    override suspend fun auth() = Obsidian.Details(
        authenticated = true,
        status = "fake-status",
        versions = Obsidian.Details.Versions(
            obsidian = "fake-obsidian-version",
            restApi = "fake-rest-api-version"
        )
    )
}
