package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.NotificationFanoutRequest
import io.tolgee.service.notification.activity.NotificationFanoutService
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.coroutines.CoroutineContext

@Component
class NotificationFanoutChunkProcessor(
  private val notificationFanoutService: NotificationFanoutService,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<NotificationFanoutRequest, Any?, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    chunk.forEach { notificationFanoutService.fanOut(it) }
  }

  override fun getTarget(data: NotificationFanoutRequest): List<Long> = listOf(data.activityRevisionId)

  override fun getParams(data: NotificationFanoutRequest): Any? = null

  override fun getParamsType(): Class<Any?>? = null

  override fun getTargetItemType(): Class<Long> = Long::class.java
}
