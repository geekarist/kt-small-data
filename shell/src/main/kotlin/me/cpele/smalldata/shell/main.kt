package me.cpele.smalldata.shell

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.smalldata.core.App
import me.cpele.smalldata.core.init
import me.cpele.smalldata.core.makeUpdate
import me.cpele.smalldata.core.view
import oolong.Dispatch
import oolong.runtime

/**
 * TODO: Use [obsidian-local-rest-api](https://github.com/coddingtonbear/obsidian-local-rest-api) to search notes
 */
@Composable
@Preview
fun MainScreen() {
    var view: App.View by remember {
        mutableStateOf(App.Model.init().model.view {
            // No op
        })
    }
    runtime(
        init = {
            val (model, effect) = App.Model.init()
            model to effect
        },
        update = { msg: App.Event, model: App.Model ->
            val (newModel, effect) = model.makeUpdate(FakeObsidian).invoke(msg)
            newModel to effect
        },
        view = { model: App.Model, dispatch: Dispatch<App.Event> -> model.view { dispatch(it) } },
        render = {
            view = it
            (Unit)
        }
    )
    MaterialTheme {
        Column {
            TextField(view.query.text, onValueChange = {
                view.query.onTextChanged(it)
            })
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
