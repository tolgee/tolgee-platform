package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner
import org.slf4j.LoggerFactory

class RedisRunner {
  private val runner = io.tolgee.misc.dockerRunner.DockerContainerRunner(
    image = "redis:6",
    expose = mapOf("56379" to "6379"),
    waitForLog = "Ready to accept connections",
    rm = true,
    name = "server-integration-test-redis",
    logStdOut = LoggerFactory.getLogger(DockerContainerRunner::class.java)::info
  )

  fun run() {
    runner.run()
  }

  fun stop() {
    runner.stop()
  }
}
