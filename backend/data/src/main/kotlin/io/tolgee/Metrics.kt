package io.tolgee

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class Metrics(
  private val meterRegistry: MeterRegistry
) {
  fun registerJobQueue(queue: ConcurrentLinkedQueue<*>) {
    Gauge.builder("tolgee.batch.job.execution.queue.size", queue) { it.size.toDouble() }
      .description("Size of the queue of batch job executions")
      .register(meterRegistry)
  }
}
