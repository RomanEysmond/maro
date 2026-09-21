package com.maro.feature.auth.presentation.registration

import com.maro.core.presentation.util.UiText

data class RegistrationState(
    val firstName: String = "",
    val lastName: String = "",
    /** National part of the number: up to 10 digits, without the "+7" prefix (the prefix is shown by the UI). */
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
) {
    /** The number in international format, e.g. "+79001234567". Used for sending the SMS code. */
    val fullPhoneNumber: String
        get() = "$COUNTRY_PREFIX$phoneNumber"

    val isContinueEnabled: Boolean
        get() = phoneNumber.length == PHONE_LENGTH && firstName.isNotBlank()

    companion object {
        const val COUNTRY_PREFIX = "+7"
        const val PHONE_LENGTH = 10
    }
}
