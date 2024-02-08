import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.smalldata.core.App
import me.cpele.smalldata.core.init
import me.cpele.smalldata.core.view

/**
 * TODO: Use [obsidian-local-rest-api](https://github.com/coddingtonbear/obsidian-local-rest-api) to search notes
 */
@Composable
@Preview
fun MainScreen() {
    var model: App.Model by remember { mutableStateOf(App.Model.init()) }
    var view: App.View by remember { mutableStateOf(model.view { TODO() }) }
    LaunchedEffect(model) {
        view = model.view { TODO() }
    }
    MaterialTheme {
        Text(view.query.text)
        Column {
            view.results.forEach {
                Text(it.text)
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MainScreen()
    }
}
