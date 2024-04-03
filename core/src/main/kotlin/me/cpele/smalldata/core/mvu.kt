package me.cpele.smalldata.core

import kotlinx.coroutines.CoroutineScope

data class Change<ModelT, EventT>(
    val model: ModelT,
    val effect: suspend CoroutineScope.((EventT) -> Unit) -> Unit = {}
)
