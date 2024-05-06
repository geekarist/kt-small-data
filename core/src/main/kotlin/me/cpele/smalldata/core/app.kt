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
        val google: Google = Google(),
    ) {
        data class Obsidian(
            val backend: ObsidianEffects.Details? = null,
            val searchJob: Job? = null,
        )

        data class Google(val searchJob: Job? = null)
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

        sealed interface GoogleSearch : Event {
            data class Launched(val job: Job) : GoogleSearch

            data class ReceivedResults(val results: List<GoogleEffects.Finding>) : GoogleSearch
        }
    }

    data class Context(
        val obsidian: ObsidianEffects,
        val logger: Logger,
        val google: GoogleEffects
    )
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
        is Event.ObsidianSearch.ReceivedResults -> changeOnObsidianSearchResults(model, event)
        is Event.ObsidianSearch.OpenFileRequested -> Change(model) { obsidian.open(event.path) }
        is Event.GoogleSearch.Launched ->
            Change(model.copy(google = model.google.copy(searchJob = event.job)))
        is Event.GoogleSearch.ReceivedResults -> changeOnGoogleSearchResults(model, event)
    }

private fun changeOnObsidianSearchResults(
    model: Model,
    event: Event.ObsidianSearch.ReceivedResults
): Change<Model, Event> = run {
    val newResultSet = event.results.map { it.label }
    val currentResultSet = model.results?.toSet()
    val updatedResultSet =
        currentResultSet?.let { actualCurrentResultSet -> actualCurrentResultSet + newResultSet }
    val updatedResultList = updatedResultSet?.toList()
    Change(model.copy(results = updatedResultList))
}

private fun changeOnGoogleSearchResults(
    model: Model,
    event: Event.GoogleSearch.ReceivedResults
): Change<Model, Event> = run {
    val newResultSet = event.results.map { it.label }
    val currentResultSet = model.results?.toSet()
    val updatedResultSet =
        currentResultSet?.let { actualCurrentResultSet -> actualCurrentResultSet + newResultSet }
    val updatedResultList = updatedResultSet?.toList()
    Change(model.copy(results = updatedResultList))
}

context(App.Context)
private fun updateOnQueryChanged(model: Model, event: Event.QueryChanged) =
    Change(model.copy(query = event.query)) { dispatch ->
        logger.info("Canceling prior Obsidian search job")
        model.obsidian.searchJob?.cancel()
        val obsidianJob = launch {
            logger.info("Obsidian search job was launched")
            logger.info("Searching notes for query: ${event.query}")
            val results: List<ObsidianEffects.Finding> = obsidian.notes(event.query)
            logger.info("Found ${results.size} Obsidian results: $results")
            val receivedObsidianResults: Event = Event.ObsidianSearch.ReceivedResults(results)
            logger.info("\"Debouncing\" Obsidian results for 1 sec")
            delay(1.seconds) // Kind of debounce
            logger.info("Dispatching event: $receivedObsidianResults")
            dispatch(receivedObsidianResults)
        }
        dispatch(Event.ObsidianSearch.Launched(job = obsidianJob))
        model.google.searchJob?.cancel()
        logger.info("Canceling prior Google search job")
        val googleJob = launch {
            logger.info("Google search job was launched")
            logger.info("Searching web for query: ${event.query}")
            val results: List<GoogleEffects.Finding> = google.search(event.query)
            logger.info("Found ${results.size} web results: $results")
            val receivedGoogleResults: Event = Event.GoogleSearch.ReceivedResults(results)
            logger.info("\"Debouncing\" Google results for 1 sec")
            delay(1.seconds)
            logger.info("Dispatching event: $receivedGoogleResults")
            dispatch(receivedGoogleResults)
        }
        dispatch(Event.GoogleSearch.Launched(job = googleJob))
    }
