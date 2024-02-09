package me.cpele.smalldata.core

interface UiModel {
    data class TextField(val text: String, val onTextChanged: (String) -> Unit)
    data class TextLabel(val text: String)
}
