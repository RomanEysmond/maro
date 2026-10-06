package com.maro.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class MediaServiceTest {

    private class FakeMediaStore : MediaStore {
        override fun uploadUrl(key: String) = "https://storage/put/$key"
        override fun downloadUrl(key: String) = "https://storage/get/$key"
    }

    private val store = FakeChatStore().apply { chats["c1"] = listOf("anna", "ivan") }
    private val service = MediaService(store, FakeMediaStore())

    @Test
    fun `a participant gets an upload URL for the message's own key`() = runTest {
        assertEquals(
            MediaResult.Url("chats/c1/m1", "https://storage/put/chats/c1/m1"),
            service.uploadUrl("anna", "c1", "m1"),
        )
    }

    @Test
    fun `someone outside the chat gets nothing`() = runTest {
        assertEquals(MediaResult.Forbidden, service.uploadUrl("eve", "c1", "m1"))
        assertEquals(MediaResult.Forbidden, service.downloadUrl("eve", "c1", "chats/c1/m1"))
    }

    @Test
    fun `an unknown chat is reported as such`() = runTest {
        assertEquals(MediaResult.ChatNotFound, service.uploadUrl("anna", "nope", "m1"))
    }

    @Test
    fun `a participant downloads the chat's files only`() = runTest {
        store.chats["c2"] = listOf("eve")

        assertEquals(
            MediaResult.Url("chats/c1/m1", "https://storage/get/chats/c1/m1"),
            service.downloadUrl("ivan", "c1", "chats/c1/m1"),
        )
        // A key of another chat, or one that tries to climb out of the chat's folder.
        assertEquals(MediaResult.Forbidden, service.downloadUrl("ivan", "c1", "chats/c2/m1"))
        assertEquals(MediaResult.Forbidden, service.downloadUrl("ivan", "c1", "chats/c1/../c2/m1"))
        assertEquals(MediaResult.Forbidden, service.downloadUrl("ivan", "c1", "chats/c1/"))
    }

    @Test
    fun `without a configured storage media is unavailable`() = runTest {
        val noStorage = MediaService(store, media = null)

        assertEquals(MediaResult.Unavailable, noStorage.uploadUrl("anna", "c1", "m1"))
    }

    @Test
    fun `the storage settings come from the environment, all or nothing`() {
        val env = mapOf(
            S3Config.ENDPOINT_ENV to "https://s3.example",
            S3Config.REGION_ENV to "eu-central-003",
            S3Config.BUCKET_ENV to "maro-media",
            S3Config.ACCESS_KEY_ENV to "id",
            S3Config.SECRET_KEY_ENV to "secret",
        )

        assertEquals("maro-media", S3Config.from(env::get)?.bucket)
        assertEquals(null, S3Config.from((env - S3Config.SECRET_KEY_ENV)::get))
    }
}
