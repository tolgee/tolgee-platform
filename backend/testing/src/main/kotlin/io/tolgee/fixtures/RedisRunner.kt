package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner

class RedisRunner {
  companion object {
    val port: String = System.getenv("TOLGEE_TEST_REDIS_PORT") ?: "56379"
    val containerName: String = System.getenv("TOLGEE_TEST_REDIS_CONTAINER") ?: "server-integration-test-redis"
  }

  private val runner =
    DockerContainerRunner(
      image = "redis:6",
      expose = mapOf(port to "6379"),
      waitForLog = "Ready to accept connections",
      rm = true,
      name = containerName,
    )

  fun run() {
    runner.run()
  }

  fun stop() {
    runner.stop()
  }
}
