package me.cpele.smalldata.shell

import me.cpele.smalldata.core.Obsidian

object RestObsidian : Obsidian {
    override fun notes(query: String): List<Obsidian.Finding> {
        TODO("Not yet implemented")
    }

    override fun auth(): Obsidian.Details {
        TODO("Not yet implemented")
    }
}