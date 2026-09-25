package com.maro.feature.chat.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.OutboxResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Sends what is still queued; WorkManager runs it once the network is up and backs off between retries. */
internal class OutboxWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: MessageRepository by inject()

    override suspend fun doWork(): Result = when (repository.flushOutbox(attempt = runAttemptCount)) {
        OutboxResult.DONE -> Result.success()
        OutboxResult.RETRY -> Result.retry()
    }
}
