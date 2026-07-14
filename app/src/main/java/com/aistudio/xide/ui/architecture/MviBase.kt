package com.aistudio.xide.ui.architecture

/**
 * Base interface for UI State in the MVI architecture.
 * Represents the current state of a screen or component.
 */
interface UiState

/**
 * Base interface for UI Events in the MVI architecture.
 * Represents user actions or system events that intent to change the state.
 */
interface UiEvent

/**
 * Base interface for UI Effects in the MVI architecture.
 * Represents one-off events like navigation, showing a snackbar, etc.
 */
interface UiEffect
