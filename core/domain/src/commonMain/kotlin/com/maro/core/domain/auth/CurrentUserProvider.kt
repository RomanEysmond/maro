package com.maro.core.domain.auth

/** Who is signed in right now. A synchronous read: the session is known from the very first frame. */
interface CurrentUserProvider {
    /** Backend user id (uid), or `null` when nobody is signed in. */
    val userId: String?
}
