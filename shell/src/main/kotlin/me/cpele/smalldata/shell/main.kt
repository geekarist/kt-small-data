package me.cpele.smalldata.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.smalldata.core.*
import oolong.Dispatch
import oolong.runtime

@Composable
private fun App.Ui(view: App.View) = run {
    val authUi: @Composable (Modifier) -> Unit = { modifier ->
        // Authentication
        view.auth.forEach { authItemUim ->
            when (authItemUim) {
                is UiModel.Button -> Button(
                    onClick = authItemUim.onPress,
                    modifier = modifier
                ) {
                    Text(authItemUim.text)
                }

                is UiModel.TextLabel -> Text(authItemUim.text)
                else -> error("Auth item view has unknown type: $authItemUim")
            }
        }
    }

    if (view.auth.size == 1) {
        // Not authenticated ⇒ only auth button
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            authUi(Modifier)
        }
    } else {
        // Authenticated ⇒ auth, results, query
        Column(modifier = Modifier.fillMaxSize()) {
            var queryText: String by remember { mutableStateOf(view.query.text) }
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                queryText = view.query.text
                focusRequester.requestFocus()
            }
            LaunchedEffect(queryText) { view.query.onTextChanged(queryText) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                var queryHeightDp by remember {
                    mutableStateOf(0.dp)
                }
                // Query
                val currentDensity = LocalDensity.current
                TextField(
                    queryText,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester).onGloballyPositioned { coords ->
                        queryHeightDp = with(currentDensity) {
                            coords.size.height.toDp()
                        }
                    },
                    placeholder = { Text(view.query.placeholder ?: "") },
                    onValueChange = { queryText = it }
                )
                authUi(Modifier.height(queryHeightDp))
            }
            // Results
            LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(view.results) {
                    Text(it.text)
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var view: App.View by remember {
            mutableStateOf(App.Model.init().model.view {
                // No op
            })
        }
        MaterialTheme {
            App.Ui(view)
        }
        LaunchedEffect(Unit) {
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
        }
    }
}
