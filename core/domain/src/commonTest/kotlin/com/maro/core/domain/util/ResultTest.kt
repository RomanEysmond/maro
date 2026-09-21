package com.maro.core.domain.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ResultTest {

    @Test
    fun `map transforms success and keeps failure`() {
        val success: Result<Int, DataError.Network> = Result.Success(2)
        val failure: Result<Int, DataError.Network> = Result.Error(DataError.Network.NO_INTERNET)

        assertThat(success.map { it * 2 }).isEqualTo(Result.Success(4))
        assertThat(failure.map { it * 2 }).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun `onSuccess and onFailure run only for the matching branch`() {
        var successCalls = 0
        var failureCalls = 0
        val success: Result<Int, DataError.Local> = Result.Success(1)
        val failure: Result<Int, DataError.Local> = Result.Error(DataError.Local.DISK_FULL)

        success.onSuccess { successCalls++ }.onFailure { failureCalls++ }
        failure.onSuccess { successCalls++ }.onFailure { failureCalls++ }

        assertThat(successCalls).isEqualTo(1)
        assertThat(failureCalls).isEqualTo(1)
    }

    @Test
    fun `asEmptyResult drops the payload`() {
        val result: Result<String, DataError.Local> = Result.Success("data")

        assertThat(result.asEmptyResult()).isEqualTo(Result.Success(Unit))
    }
}
