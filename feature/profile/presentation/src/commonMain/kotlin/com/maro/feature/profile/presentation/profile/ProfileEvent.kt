package com.maro.feature.profile.presentation.profile

sealed interface ProfileEvent {
    data object NavigateToEdit : ProfileEvent
}
