package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner

class RedisRunner {
  private val runner =
    DockerContainerRunner(
      image = "redis:6",
      expose = mapOf("56379" to "6379"),
      waitForLog = "Ready to accept connections",
      rm = true,
      name = "server-integration-test-redis",
    )

  fun run() {
    runner.run()
  }

  fun stop() {
    runner.stop()
  }
}
