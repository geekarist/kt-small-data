object App {
    data class Model(val query: String? = null, val results: List<String>? = null)

    data class View(val query: UiModel.TextField, val results: List<UiModel.TextLabel>)

    sealed interface Event {
        data class QueryChanged(val query: String) : Event
        data class ReceivedResults(val results: List<Obsidian.Finding>)
    }
}

typealias Change<ModelT, EventT> = Pair<ModelT, suspend ((EventT) -> Unit) -> Unit>

fun App.Model.view(dispatch: (App.Event) -> Unit): App.View = App.View(
    UiModel.TextField(query ?: "") {
        dispatch(Event.QueryChanged(query))
    },
    results?.map {
        UiModel.TextLabel(it)
    } ?: emptyList()
)

fun App.Model.update(event: App.Event): Change<App.Model, App.Event> = when (event) {
    is App.Event.QueryChanged -> Change(copy(query = event.query)) { dispatch ->
        val results: List<Obsidian.Finding> = Obsidian.find(query)
        val receivedResults = Event.ReceivedResults(results)
        dispatch(receivedResults)
    }
}
