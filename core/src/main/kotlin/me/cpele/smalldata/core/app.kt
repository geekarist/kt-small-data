package me.cpele.smalldata.core

object App {
    data class Model(val query: String? = null, val results: List<String>? = null) {
        companion object
    }

    data class View(val query: UiModel.TextField, val results: List<UiModel.TextLabel>)

    sealed interface Event {
        data class QueryChanged(val query: String) : Event
        data class ReceivedResults(val results: List<Obsidian.Finding>) : Event
    }
}

fun App.Model.Companion.init(): Change<App.Model, App.Event> = Change(App.Model())

fun App.Model.view(dispatch: (App.Event) -> Unit): App.View = run {
    val queryOrBlank = query ?: ""
    val placeholder = "Search your data"
    val queryUim = UiModel.TextField(queryOrBlank, placeholder) { newQuery ->
        dispatch(App.Event.QueryChanged(newQuery))
    }
    val resultsUim = results?.map { UiModel.TextLabel(it) } ?: emptyList()
    App.View(queryUim, resultsUim)
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

        is App.Event.ReceivedResults -> Change(copy(results = event.results.map { finding ->
            finding.label
        }))
    }
}
