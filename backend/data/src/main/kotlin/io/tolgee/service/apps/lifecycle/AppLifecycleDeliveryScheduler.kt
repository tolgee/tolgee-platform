package io.tolgee.service.apps.lifecycle

import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.util.Logging
import io.tolgee.util.runSentryCatching
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AppLifecycleDeliveryScheduler(
  private val dispatcher: AppLifecycleDeliveryDispatcher,
  private val lockingProvider: LockingProvider,
  private val schedulingManager: SchedulingManager,
  private val appsProperties: AppsProperties,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun scheduleRetries() {
    if (!appsProperties.enabled) return
    schedulingManager.scheduleWithFixedDelay(::retry, RETRY_PERIOD)
  }

  /**
   * Retries are per-process — each node only knows the deliveries it is holding — but the sweep of
   * deliveries no process holds any more writes rows every node can see, so it takes the lock.
   */
  fun retry() {
    dispatcher.retryDue()
    lockingProvider.withLockingIfFree(ABANDON_LOCK_NAME, ABANDON_LOCK_LEASE_TIME) {
      runSentryCatching {
        dispatcher.abandonStale()
      }
    }
  }

  companion object {
    private const val ABANDON_LOCK_NAME = "app_delivery_abandon_lock"
    private val RETRY_PERIOD = Duration.ofSeconds(30)
    private val ABANDON_LOCK_LEASE_TIME = Duration.ofMinutes(5)
  }
}
