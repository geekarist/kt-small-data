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
        val obsidianBackend: Obsidian.Details? = null,
        val obsidianSearchJob: Job? = null,
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

        sealed interface ObsidianSearch : Event {
            data class AuthReceived(val auth: Obsidian.Details) : ObsidianSearch

            data class Launched(val job: Job) : ObsidianSearch

            data class ReceivedResults(val results: List<Obsidian.Finding>) : ObsidianSearch

            data class OpenFileRequested(val path: String) : ObsidianSearch
        }
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
        if (model.obsidianBackend?.authenticated == true) {
            listOf(
                UiModel.TextLabel("Status: ${model.obsidianBackend.status}"),
                UiModel.TextLabel("Obsidian: ${model.obsidianBackend.versions.obsidian}"),
                UiModel.TextLabel("REST API: ${model.obsidianBackend.versions.self}"),
            )
        } else {
            listOf(UiModel.Button("Authenticate") { dispatch(App.Event.AuthRequested) })
        }

    val resultsUim =
        model.results?.map {
            UiModel.Button(it) { dispatch(App.Event.ObsidianSearch.OpenFileRequested(it)) }
        } ?: emptyList()
    App.View(queryUim, authUim, resultsUim)
}

context(App.Context)
fun App.update(model: App.Model, event: App.Event): Change<App.Model, App.Event> =
    when (event) {
        App.Event.AuthRequested ->
            Change(model) { dispatch ->
                val auth = obsidian.auth()
                dispatch(App.Event.ObsidianSearch.AuthReceived(auth))
            }
        is App.Event.QueryChanged -> updateOnQueryChanged(model, event)
        is App.Event.ObsidianSearch.AuthReceived ->
            Change(model = model.copy(obsidianBackend = event.auth))
        is App.Event.ObsidianSearch.Launched -> Change(model.copy(obsidianSearchJob = event.job))
        is App.Event.ObsidianSearch.ReceivedResults ->
            Change(model.copy(results = event.results.map { finding -> finding.label }))
        is App.Event.ObsidianSearch.OpenFileRequested -> Change(model) { obsidian.open(event.path) }
    }

private fun App.Context.updateOnQueryChanged(model: App.Model, event: App.Event.QueryChanged) =
    Change(model.copy(query = event.query)) { dispatch ->
        logger.info("Canceling prior Obsidian search job")
        model.obsidianSearchJob?.cancel()
        val obsidianJob = launch {
            logger.info("Query-changed job was launched")
            logger.info("Searching notes for query: ${event.query}")
            val results: List<Obsidian.Finding> = obsidian.notes(event.query)
            logger.info("Found ${results.size} results: $results")
            val receivedObsidianResults: App.Event =
                App.Event.ObsidianSearch.ReceivedResults(results)
            logger.info("\"Debouncing\" for 1 sec")
            delay(1.seconds) // Kind of debounce
            logger.info("Dispatching event: $receivedObsidianResults")
            dispatch(receivedObsidianResults)
        }
        dispatch(App.Event.ObsidianSearch.Launched(job = obsidianJob))
    }
