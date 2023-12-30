package io.tolgee.postgresRunners

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PostgresStopper(
  private val postgresAutostartProperties: PostgresAutostartProperties,
  private val postgresRunner: PostgresRunner?,
) {
  @EventListener(ContextClosedEvent::class)
  fun onAppStop() {
    val itHasToStop = postgresAutostartProperties.mode != PostgresAutostartProperties.PostgresAutostartMode.DOCKER
    val itShouldStop = postgresAutostartProperties.stop
    if (itHasToStop || itShouldStop) {
      postgresRunner?.stop()
    }
  }
}
