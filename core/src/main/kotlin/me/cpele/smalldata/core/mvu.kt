package me.cpele.smalldata.core

data class Change<ModelT, EventT>(
    val model: ModelT,
    val effect: suspend ((EventT) -> Unit) -> Unit = {}
)