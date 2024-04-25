package me.cpele.smalldata.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.cpele.smalldata.core.App.Event
import me.cpele.smalldata.core.App.Model
import me.cpele.smalldata.core.App.View
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

object App {
    data class Model(
        val query: String? = null,
        val results: List<String>? = null,
        val obsidian: Obsidian = Obsidian(),
    ) {
        data class Obsidian(
            val backend: ObsidianEffects.Details? = null,
            val searchJob: Job? = null,
        )
    }

    data class View(
        val query: UiModel.TextField,
        val auth: List<UiModel>,
        val results: List<UiModel.Button>
    )

    sealed interface Event {
        data object AuthRequested : Event

        data class QueryChanged(val query: String) : Event

        sealed interface ObsidianSearch : Event {
            data class AuthReceived(val auth: ObsidianEffects.Details) : ObsidianSearch

            data class Launched(val job: Job) : ObsidianSearch

            data class ReceivedResults(val results: List<ObsidianEffects.Finding>) : ObsidianSearch

            data class OpenFileRequested(val path: String) : ObsidianSearch
        }
    }

    data class Context(val obsidian: ObsidianEffects, val logger: Logger)
}

fun App.init(): Change<Model, Event> = run {
    Change(Model()) { dispatch -> dispatch(Event.AuthRequested) }
}

fun App.view(model: Model, dispatch: (Event) -> Unit): View = run {
    val queryOrBlank = model.query ?: ""
    val placeholder = "Search your data"
    val queryUim =
        UiModel.TextField(queryOrBlank, placeholder) { newQuery ->
            dispatch(Event.QueryChanged(newQuery))
        }

    val authUim =
        if (model.obsidian.backend?.authenticated == true) {
            listOf(
                UiModel.TextLabel("Status: ${model.obsidian.backend.status}"),
                UiModel.TextLabel("Obsidian: ${model.obsidian.backend.versions.obsidian}"),
                UiModel.TextLabel("REST API: ${model.obsidian.backend.versions.self}"),
            )
        } else {
            listOf(UiModel.Button("Authenticate") { dispatch(Event.AuthRequested) })
        }

    val resultsUim =
        model.results?.map {
            UiModel.Button(it) { dispatch(Event.ObsidianSearch.OpenFileRequested(it)) }
        } ?: emptyList()
    View(queryUim, authUim, resultsUim)
}

context(App.Context)
fun App.update(model: Model, event: Event): Change<Model, Event> =
    when (event) {
        Event.AuthRequested ->
            Change(model) { dispatch ->
                val auth = obsidian.auth()
                dispatch(Event.ObsidianSearch.AuthReceived(auth))
            }
        is Event.QueryChanged -> updateOnQueryChanged(model, event)
        is Event.ObsidianSearch.AuthReceived ->
            Change(model.copy(obsidian = model.obsidian.copy(backend = event.auth)))
        is Event.ObsidianSearch.Launched ->
            Change(model.copy(obsidian = model.obsidian.copy(searchJob = event.job)))
        is Event.ObsidianSearch.ReceivedResults ->
            Change(model.copy(results = event.results.map { finding -> finding.label }))
        is Event.ObsidianSearch.OpenFileRequested -> Change(model) { obsidian.open(event.path) }
    }

context(App.Context)
private fun updateOnQueryChanged(model: Model, event: Event.QueryChanged) =
    Change(model.copy(query = event.query)) { dispatch ->
        logger.info("Canceling prior Obsidian search job")
        model.obsidian.searchJob?.cancel()
        val obsidianJob = launch {
            logger.info("Query-changed job was launched")
            logger.info("Searching notes for query: ${event.query}")
            val results: List<ObsidianEffects.Finding> = obsidian.notes(event.query)
            logger.info("Found ${results.size} results: $results")
            val receivedObsidianResults: Event = Event.ObsidianSearch.ReceivedResults(results)
            logger.info("\"Debouncing\" for 1 sec")
            delay(1.seconds) // Kind of debounce
            logger.info("Dispatching event: $receivedObsidianResults")
            dispatch(receivedObsidianResults)
        }
        dispatch(Event.ObsidianSearch.Launched(job = obsidianJob))
    }
