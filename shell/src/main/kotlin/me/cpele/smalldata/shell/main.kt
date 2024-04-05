package me.cpele.smalldata.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.json.Json
import me.cpele.smalldata.core.*
import oolong.Dispatch
import oolong.runtime
import java.util.logging.Logger
import kotlin.math.min

@Composable
private fun List<UiModel>.AuthUi(modifier: Modifier = Modifier) = run {
    // Authentication
    Column {
        this@AuthUi.forEach { authItemUim ->
            when (authItemUim) {
                is UiModel.Button ->
                    Button(onClick = authItemUim.onPress, modifier = modifier) {
                        Text(authItemUim.text)
                    }
                is UiModel.TextLabel -> Text(authItemUim.text)
                else -> error("Auth item view has unknown type: $authItemUim")
            }
        }
    }
}

@Composable
private fun UiModel.TextField.Ui(modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    var query by remember { mutableStateOf(this.text) }
    LaunchedEffect(query) { this@Ui.onTextChanged(query) }
    TextField(
        modifier = modifier.focusRequester(focusRequester),
        value = query,
        onValueChange = { query = it })
}

@Composable
private fun App.Ui(view: App.View) = run {
    MaterialTheme(
        colors =
            MaterialTheme.colors.copy(
                surface =
                    Color.LightGray.copy(
                        red = min(1f, Color.LightGray.red * 1.2f),
                        green = min(1f, Color.LightGray.green * 1.2f),
                        blue = min(1f, Color.LightGray.blue * 1.2f),
                    ),
            )) {
            Surface {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                            var queryHeight: Dp? by remember { mutableStateOf(null) }
                            view.query.Ui(
                                Modifier.weight(1f).onGloballyPositioned {
                                    queryHeight = it.size.height.dp
                                })
                            view.auth.AuthUi(
                                Modifier.let { authMod ->
                                    queryHeight?.let { height -> authMod.height(height) } ?: authMod
                                })
                        }
                    Divider()
                    LazyColumn(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start) {
                            view.results.forEach { result ->
                                item {
                                    Card(Modifier.fillMaxWidth().align(Alignment.Start)) {
                                        TextButton(
                                            modifier = Modifier.align(Alignment.Start),
                                            onClick = result.onPress) {
                                                Text(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    text = result.text)
                                            }
                                    }
                                }
                            }
                        }
                }
            }
        }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var view: App.View by remember {
            mutableStateOf(
                App.Model.init().model.view {
                    // No op
                })
        }
        MaterialTheme { App.Ui(view) }
        LaunchedEffect(Unit) {
            runtime(
                init = {
                    val (model, effect) = App.Model.init()
                    model to effect
                },
                update = { msg: App.Event, model: App.Model ->
                    val obsidian = makeObsidian(makeJson())
                    val logger = Logger.getLogger(App::class.simpleName)
                    val context = App.Context(obsidian, logger)
                    val (newModel, effect) = with(context) { model.update(msg) }
                    newModel to effect
                },
                view = { model: App.Model, dispatch: Dispatch<App.Event> ->
                    model.view { dispatch(it) }
                },
                render = {
                    view = it
                    (Unit)
                })
        }
    }
}

fun makeJson() = Json { ignoreUnknownKeys = true }

fun makeObsidian(json: Json): Obsidian = run {
    val fakeObsidianStr = System.getenv("FAKE_OBSIDIAN")
    if (fakeObsidianStr.isNullOrBlank()) {
        RestObsidian(json)
    } else {
        FakeObsidian
    }
}
