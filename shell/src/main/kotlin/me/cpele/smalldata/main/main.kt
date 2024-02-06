import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * TODO: Use [obsidian-local-rest-api](https://github.com/coddingtonbear/obsidian-local-rest-api) to search notes
 */
@Composable
@Preview
fun MainScreen() {
    var view: AppView
    var model: AppModel
    LaunchedEffect(appModel) {
        view = appView(model)
    }
    AppRender(view) { dispatch: (AppEvent) -> Unit
        TODO()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MainScreen()
    }
}
