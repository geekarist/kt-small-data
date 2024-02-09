package me.cpele.smalldata.shell

import me.cpele.smalldata.core.Obsidian

object FakeObsidian : Obsidian {
    override fun findNotes(query: String): List<Obsidian.Finding> = (1..10).map { num ->
        Obsidian.Finding("Note n°$num: $query")
    }
}
