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

    data class View(
        val query: UiModel.TextField,
        val auth: List<UiModel>,
        val results: List<UiModel.Button>
    )

    sealed interface Event {
        data object AuthRequested : Event

        data class QueryChanged(val query: String) : Event

        data class ReceivedResults(val results: List<Obsidian.Finding>) : Event

        data class AuthReceived(val auth: Obsidian.Details) : Event

        data class SearchLaunched(val job: Job) : Event

        data class OpenFileRequested(val path: String) : Event
    }

    data class Context(val obsidian: Obsidian, val logger: Logger)
}

fun App.init(): Change<App.Model, App.Event> = run {
    Change(App.Model()) { dispatch -> dispatch(App.Event.AuthRequested) }
}

fun App.view(model: App.Model, dispatch: (App.Event) -> Unit): App.View = run {
    val queryOrBlank = model.query ?: ""
    val placeholder = "Search your data"
    val queryUim =
        UiModel.TextField(queryOrBlank, placeholder) { newQuery ->
            dispatch(App.Event.QueryChanged(newQuery))
        }

    val authUim =
        if (model.backend?.authenticated == true) {
            listOf(
                UiModel.TextLabel("Status: ${model.backend.status}"),
                UiModel.TextLabel("Obsidian: ${model.backend.versions.obsidian}"),
                UiModel.TextLabel("REST API: ${model.backend.versions.self}"),
            )
        } else {
            listOf(UiModel.Button("Authenticate") { dispatch(App.Event.AuthRequested) })
        }

    val resultsUim =
        model.results?.map { UiModel.Button(it) { dispatch(App.Event.OpenFileRequested(it)) } }
            ?: emptyList()
    App.View(queryUim, authUim, resultsUim)
}

context(App.Context)
fun App.update(model: App.Model, event: App.Event): Change<App.Model, App.Event> =
    when (event) {
        App.Event.AuthRequested ->
            Change(model) { dispatch ->
                val auth = obsidian.auth()
                dispatch(App.Event.AuthReceived(auth))
            }
        is App.Event.AuthReceived -> Change(model = model.copy(backend = event.auth))
        is App.Event.QueryChanged ->
            Change(model.copy(query = event.query)) { dispatch ->
                logger.info("Canceling prior search job")
                model.searchJob?.cancel()
                val job = launch {
                    logger.info("Query-changed job was launched")
                    logger.info("Searching notes for query: ${event.query}")
                    val results: List<Obsidian.Finding> = obsidian.notes(event.query)
                    logger.info("Found ${results.size} results: $results")
                    val receivedResults: App.Event = App.Event.ReceivedResults(results)
                    logger.info("\"Debouncing\" for 1 sec")
                    delay(1.seconds) // Kind of debounce
                    logger.info("Dispatching event: $receivedResults")
                    dispatch(receivedResults)
                }
                dispatch(App.Event.SearchLaunched(job = job))
            }
        is App.Event.ReceivedResults ->
            Change(model.copy(results = event.results.map { finding -> finding.label }))
        is App.Event.SearchLaunched -> Change(model.copy(searchJob = event.job))
        is App.Event.OpenFileRequested -> Change(model) { obsidian.open(event.path) }
    }
