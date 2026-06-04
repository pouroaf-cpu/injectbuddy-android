package com.injectbuddy.android.core

/**
 * Standard screen state envelope. Every networked screen has Loading / Empty / Error /
 * Content cases (see SCREENS.md). [Content] carries the loaded data.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}
