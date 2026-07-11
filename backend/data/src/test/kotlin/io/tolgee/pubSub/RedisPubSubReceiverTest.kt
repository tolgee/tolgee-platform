package io.tolgee.pubSub

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.JobQueueItemsEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate

class RedisPubSubReceiverTest {
  @Test
  fun `deserializes a queue event missing jobType and ignores unknown fields`() {
    val publisher = mock<ApplicationEventPublisher>()
    val objectMapper =
      jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val receiver = RedisPubSubReceiver(mock<SimpMessagingTemplate>(), publisher, objectMapper)

    // Payload as produced by an OLDER instance: no jobType, plus a hypothetical unknown field.
    val json =
      """{"items":[{"chunkExecutionId":1,"jobId":2,"executeAfter":null,""" +
        """"jobCharacter":"FAST","unknownFutureField":"x"}],"type":"ADD"}"""

    receiver.receiveJobQueueMessage(json)

    val captor = argumentCaptor<JobQueueItemsEvent>()
    verify(publisher).publishEvent(captor.capture())
    val item = captor.firstValue.items.single()
    assertThat(item.chunkExecutionId).isEqualTo(1)
    assertThat(item.jobType).isEqualTo(BatchJobType.NO_OP)
  }
}
