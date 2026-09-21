package com.maro.feature.profile.presentation.edit

sealed interface EditProfileEvent {
    /** Saved, skipped or dismissed: the screen is done. */
    data object Close : EditProfileEvent
}
