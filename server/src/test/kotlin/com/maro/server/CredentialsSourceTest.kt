package com.maro.server

import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialsSourceTest {

    @Test
    fun `the key in the environment variable wins`() {
        val env = mapOf(CredentialsSource.CREDENTIALS_JSON_ENV to "{\"type\":\"service_account\"}")

        assertEquals(CredentialsSource.Json("{\"type\":\"service_account\"}"), CredentialsSource.from(env::get))
    }

    @Test
    fun `without it the default credentials are used`() {
        assertEquals(CredentialsSource.ApplicationDefault, CredentialsSource.from { null })
    }

    @Test
    fun `a blank variable counts as missing`() {
        val env = mapOf(CredentialsSource.CREDENTIALS_JSON_ENV to "  ")

        assertEquals(CredentialsSource.ApplicationDefault, CredentialsSource.from(env::get))
    }
}
