package io.tolgee.fixtures

class RedisRunner {
  private val runner = DockerContainerRunner(
    image = "redis:6",
    expose = mapOf("56379" to "6379"),
    name = "server-integration-test-redis",
    waitForLog = "Ready to accept connections"
  )

  fun run() {
    runner.run()
  }

  fun stop() {
    runner.stop()
  }
}
