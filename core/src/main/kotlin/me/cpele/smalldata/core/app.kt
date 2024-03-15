package me.cpele.smalldata.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

object App {
    data class Model(
        val query: String? = null,
        val results: List<String>? = null,
        val backend: Obsidian.Details? = null,
        val searchJob: Job? = null,
    ) {

        companion object
    }

    data class View(val query: UiModel.TextField, val auth: List<UiModel>, val results: List<UiModel.TextLabel>)

    sealed interface Event {
        data object AuthRequested : Event
        data class QueryChanged(val query: String) : Event
        data class ReceivedResults(val results: List<Obsidian.Finding>) : Event
        data class AuthReceived(val auth: Obsidian.Details) : Event
        data class SearchLaunched(val job: Job) : Event
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
            UiModel.TextLabel("REST API: ${this.backend.versions.self}"),
        )
    } else {
        listOf(UiModel.Button("Authenticate") {
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
        App.Event.AuthRequested -> Change(this) { dispatch ->
            val auth = obsidian.auth()
            dispatch(App.Event.AuthReceived(auth))
        }

        is App.Event.AuthReceived -> Change(model = this.copy(backend = event.auth))

        is App.Event.QueryChanged -> Change(copy(query = event.query)) { dispatch ->
            Logger.getLogger(App::class.simpleName).info("Canceling prior search job")
            this@makeUpdate.searchJob?.cancel()
            val job = launch {
                Logger.getLogger(App::class.simpleName).info("Query-changed job was launched")
                Logger.getLogger(App::class.simpleName).info("Searching notes for query: ${event.query}")
                val results: List<Obsidian.Finding> = obsidian.notes(event.query)
                Logger.getLogger(App::class.simpleName).info("Found ${results.size} results: $results")
                val receivedResults: App.Event = App.Event.ReceivedResults(results)
                Logger.getLogger(App::class.simpleName).info("\"Debouncing\" for 1 sec")
                delay(1.seconds) // Kind of debounce
                Logger.getLogger(App::class.simpleName).info("Dispatching event: $receivedResults")
                dispatch(receivedResults)
            }
            dispatch(App.Event.SearchLaunched(job = job))
        }

        is App.Event.ReceivedResults -> Change(copy(results = event.results.map { finding ->
            finding.label
        }))

        is App.Event.SearchLaunched -> Change(copy(searchJob = event.job))
    }
}
