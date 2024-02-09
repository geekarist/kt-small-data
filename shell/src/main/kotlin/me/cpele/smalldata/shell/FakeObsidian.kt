package me.cpele.smalldata.shell

import me.cpele.smalldata.core.Obsidian

object FakeObsidian : Obsidian {
    override fun findNotes(query: String): List<Obsidian.Finding> {
        TODO("Not yet implemented")
    }
}
