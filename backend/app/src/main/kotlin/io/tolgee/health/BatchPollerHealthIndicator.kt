package io.tolgee.health

import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthContributor
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component("batchApiPoller")
class BatchPollerHealthIndicator(
  private val openAiBatchJobTrackerRepository: OpenAiBatchJobTrackerRepository,
  private val batchProperties: BatchProperties,
) : HealthIndicator,
  HealthContributor {
  override fun health(): Health {
    val activeStatuses =
      listOf(
        OpenAiBatchTrackerStatus.SUBMITTED,
        OpenAiBatchTrackerStatus.IN_PROGRESS,
      )
    val activeCount = openAiBatchJobTrackerRepository.countByStatusIn(activeStatuses)

    return Health
      .up()
      .withDetail("activeTrackers", activeCount)
      .withDetail("maxConcurrentGlobal", batchProperties.batchApiMaxConcurrentGlobal)
      .build()
  }
}
