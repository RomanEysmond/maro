package com.maro.core.domain.auth

import kotlinx.coroutines.flow.StateFlow

/** Who is signed in right now. A synchronous read: the session is known from the very first frame. */
interface CurrentUserProvider {
    /** Backend user id (uid), or `null` when nobody is signed in. */
    val userId: String?

    /**
     * The same, as it changes: sign-in, sign-out, another account in the same process. Anything that lives longer than
     * one session (a singleton's listener, a query bound to the user) follows this instead of reading [userId] once.
     */
    val userIdFlow: StateFlow<String?>
}
