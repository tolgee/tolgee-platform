package io.tolgee.unit

import io.tolgee.batch.JobCharacter
import io.tolgee.batch.WebsocketProgressInfo
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.assert
import io.tolgee.websocket.ActivityWebsocketListener
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventType
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.Date

class ActivityWebsocketListenerBatchJobTest {
  private val published = mutableListOf<Pair<String, WebsocketEvent>>()

  private val listener =
    ActivityWebsocketListener(
      websocketEventPublisher = { destination, message -> published.add(destination to message) },
      simpleUserAccountModelAssembler = mock(),
      userAccountService = mock(),
      relationDescriptionExtractor = mock(),
      currentDateProvider = mock { on { date } doReturn Date() },
    )

  @Test
  fun `failed hidden fanout job sends no progress event, failed hidden job of a visible type does`() {
    listener.onBatchJobFailed(OnBatchJobFailed(failedHiddenJob(1, BatchJobType.NOTIFICATION_FANOUT), null))
    listener.onBatchJobFailed(OnBatchJobFailed(failedHiddenJob(2, BatchJobType.NO_OP), null))

    published.map { it.first }.assert.containsExactly(WebsocketEventType.BATCH_JOB_PROGRESS.projectDestinationFor(10))
    published.map { (it.second.data as WebsocketProgressInfo).jobId }.assert.containsExactly(2L)
  }

  private fun failedHiddenJob(
    id: Long,
    type: BatchJobType,
  ) = BatchJobDto(
    id = id,
    projectId = 10,
    authorId = null,
    target = listOf(1L),
    totalItems = 1,
    totalChunks = 1,
    chunkSize = 1,
    status = BatchJobStatus.FAILED,
    type = type,
    params = null,
    maxPerJobConcurrency = 1,
    jobCharacter = JobCharacter.FAST,
    hidden = true,
    debouncingKey = null,
  )
}
