package me.cpele.smalldata.core

interface UiModel {
    class TextField(val text: String, function: () -> Unit) {

    }

    class TextLabel(val text: String) {

    }

}
