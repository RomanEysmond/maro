package com.maro.feature.profile.presentation.edit

import com.maro.core.domain.profile.BirthDate

sealed interface EditProfileAction {
    data class OnFirstNameChange(val value: String) : EditProfileAction
    data class OnLastNameChange(val value: String) : EditProfileAction
    data class OnUsernameChange(val value: String) : EditProfileAction
    data class OnBioChange(val value: String) : EditProfileAction
    data class OnBirthDateChange(val value: BirthDate?) : EditProfileAction
    data object OnSaveClick : EditProfileAction
    data object OnSkipClick : EditProfileAction
    data object OnBackClick : EditProfileAction
}
