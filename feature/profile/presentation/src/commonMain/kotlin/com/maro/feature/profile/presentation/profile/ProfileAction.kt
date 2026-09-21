package com.maro.feature.profile.presentation.profile

sealed interface ProfileAction {
    data object OnEditClick : ProfileAction
    data object OnRetryClick : ProfileAction
}
