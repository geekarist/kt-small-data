typealias Change<ModelT, EventT> = Pair<ModelT, suspend ((EventT) -> Unit) -> Unit>

data class AppModel(val name)

sealed interface AppEvent {
    data class Blank : AppEvent
    data class Pending : AppEvent
    data class Ready : AppEvent
}

fun appView(appModel: AppModel, dispatch: (AppEvent) -> Unit): AppView = TODO()

fun appUpdate(appModel: AppModel, appEvent: AppEvent) -> Change<AppModel, AppEvent> = TODO()