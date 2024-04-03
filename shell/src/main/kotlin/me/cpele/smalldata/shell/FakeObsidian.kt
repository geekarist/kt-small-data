package me.cpele.smalldata.shell

import me.cpele.smalldata.core.Obsidian

object FakeObsidian : Obsidian {
    override suspend fun notes(query: String): List<Obsidian.Finding> =
        (1..10).map { num ->
            object : Obsidian.Finding {
                override val label: String = "Note n°$num: $query"
            }
        }

    override suspend fun auth() =
        Details(
            authenticated = true,
            status = "fake-status",
            service = "fake-service",
            versions =
                Details.Versions(
                    obsidian = "fake-obsidian-version", self = "fake-rest-api-version"))

    override suspend fun open(path: String) {
        TODO("Not yet implemented")
    }

    data class Details(
        override val status: String,
        override val versions: Versions,
        override val service: String,
        override val authenticated: Boolean
    ) : Obsidian.Details {
        data class Versions(override val obsidian: String, override val self: String) :
            Obsidian.Details.Versions
    }
}
