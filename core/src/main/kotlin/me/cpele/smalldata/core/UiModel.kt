package me.cpele.smalldata.core

// TODO: Rename to Uim
sealed interface UiModel {
    data class TextField(
        val text: String,
        val placeholder: String? = null,
        val onTextChanged: (String) -> Unit
    ) : UiModel

    data class TextLabel(val text: String) : UiModel
    data class Button(val text: String, val onPress: () -> Unit) : UiModel
}
