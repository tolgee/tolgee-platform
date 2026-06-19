package io.tolgee.pubSub

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.events.JobCancelEvent
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.websocket.RedisWebsocketEventWrapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate

class RedisPubSubReceiver(
  private val template: SimpMessagingTemplate,
  private val applicationEventPublisher: ApplicationEventPublisher,
) : Logging {
  companion object {
    // Queue events are exchanged between mixed-version instances during rolling deploys.
    // Ignore unknown fields so an added field on a newer instance doesn't break an older
    // reader; missing fields fall back to ExecutionQueueItem defaults. The 60s
    // populateQueue() re-read corrects any transient grouping drift.
    private val jobQueueMapper =
      jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  fun receiveWebsocketMessage(message: String) {
    val data = jacksonObjectMapper().readValue(message, RedisWebsocketEventWrapper::class.java)
    data.message?.let {
      template.convertAndSend(data.destination, it)
      logger.debug("Sending message to ${data.destination}")
    }
  }

  fun receiveJobQueueMessage(message: String) {
    val data = jobQueueMapper.readValue(message, JobQueueItemsEvent::class.java)
    applicationEventPublisher.publishEvent(data)
  }

  fun receiveJobCancel(message: String) {
    val data = jacksonObjectMapper().readValue(message, Long::class.java)
    applicationEventPublisher.publishEvent(JobCancelEvent(data))
  }
}
