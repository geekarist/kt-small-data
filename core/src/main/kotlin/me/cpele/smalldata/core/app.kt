package me.cpele.smalldata.core

import java.util.logging.Logger

object App {
    data class Model(
        val query: String? = null,
        val results: List<String>? = null,
        val backend: Obsidian.Details? = null,
    ) {
        companion object
    }

    data class View(val query: UiModel.TextField, val auth: List<UiModel>, val results: List<UiModel.TextLabel>)

    sealed interface Event {
        data object AuthRequested : Event
        data class QueryChanged(val query: String) : Event
        data class ReceivedResults(val results: List<Obsidian.Finding>) : Event
    }
}

fun App.Model.Companion.init(): Change<App.Model, App.Event> = Change(App.Model())

fun App.Model.view(dispatch: (App.Event) -> Unit): App.View = run {
    val queryOrBlank = this.query ?: ""
    val placeholder = "Search your data"
    val queryUim = UiModel.TextField(queryOrBlank, placeholder) { newQuery ->
        dispatch(App.Event.QueryChanged(newQuery))
    }

    val authUim = if (this.backend?.authenticated == true) {
        listOf(
            UiModel.TextLabel("Status: ${this.backend.status}"),
            UiModel.TextLabel("Obsidian: ${this.backend.versions.obsidian}"),
            UiModel.TextLabel("REST API: ${this.backend.versions.restApi}"),
        )
    } else {
        listOf(UiModel.Button("Authenticate to query") {
            dispatch(App.Event.AuthRequested)
        })
    }
    val resultsUim = this.results?.map { UiModel.TextLabel(it) } ?: emptyList()
    App.View(queryUim, authUim, resultsUim)
}

fun App.Model.makeUpdate(
    obsidian: Obsidian
): (App.Event) -> Change<App.Model, App.Event> = { event ->
    when (event) {
        is App.Event.QueryChanged -> Change(copy(query = event.query)) { dispatch ->
            val results: List<Obsidian.Finding> = obsidian.findNotes(event.query)
            val receivedResults: App.Event = App.Event.ReceivedResults(results)
            dispatch(receivedResults)
        }

        App.Event.AuthRequested -> Change(this) {
            Logger.getAnonymousLogger().info("TODO: Implement auth")
        }

        is App.Event.ReceivedResults -> Change(copy(results = event.results.map { finding ->
            finding.label
        }))
    }
}
