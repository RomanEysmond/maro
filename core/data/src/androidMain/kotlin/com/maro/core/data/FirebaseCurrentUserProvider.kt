package com.maro.core.data

import com.google.firebase.auth.FirebaseAuth
import com.maro.core.domain.auth.CurrentUserProvider

internal class FirebaseCurrentUserProvider(private val auth: FirebaseAuth) : CurrentUserProvider {
    override val userId: String? get() = auth.currentUser?.uid
}
