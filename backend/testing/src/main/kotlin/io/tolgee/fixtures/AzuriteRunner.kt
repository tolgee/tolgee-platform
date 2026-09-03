package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

class AzuriteRunner {
  companion object {
    const val IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.37.0"

    private val configuredPort: String? = System.getenv("TOLGEE_TEST_AZURITE_PORT")

    val containerName: String =
      System.getenv("TOLGEE_TEST_AZURITE_CONTAINER") ?: "server-integration-test-azurite-${UUID.randomUUID()}"

    /** Known after [run]: the configured port, or the one Docker published. */
    lateinit var port: String
      private set

    /** Microsoft's published Azurite development account, so the key below is not a secret. */
    val connectionString: String
      get() =
        "DefaultEndpointsProtocol=http;" +
          "AccountName=devstoreaccount1;" +
          "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
          "BlobEndpoint=http://127.0.0.1:$port/devstoreaccount1;"
  }

  private val runner =
    DockerContainerRunner(
      image = IMAGE,
      expose = mapOf((configuredPort ?: "0") to "10000"),
      waitForLog = "Azurite Blob service successfully listens on",
      rm = true,
      name = containerName,
      command = "azurite-blob --blobHost 0.0.0.0 --blobPort 10000",
    )

  fun run() {
    pullImageIfMissing()
    runner.run()
    port = configuredPort ?: publishedPort()
    waitForBlobEndpoint()
  }

  fun stop() {
    runner.stop()
  }

  /**
   * A cold pull inside `docker run` can exceed DockerContainerRunner's command timeout on a slow CI runner.
   */
  private fun pullImageIfMissing() {
    if (docker("image", "inspect", IMAGE).exitValue == 0) {
      return
    }
    val pull = docker("pull", IMAGE, timeoutMinutes = 10)
    check(pull.exitValue == 0) { "docker pull $IMAGE failed:\n${pull.output}" }
  }

  private fun publishedPort(): String {
    val published = docker("port", containerName, "10000")
    check(published.exitValue == 0) { "docker port $containerName failed:\n${published.output}" }
    return published.output
      .lineSequence()
      .first()
      .substringAfterLast(':')
      .trim()
  }

  /**
   * The container log says "listening" before the published port accepts connections on Docker Desktop.
   */
  private fun waitForBlobEndpoint() {
    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    val request =
      HttpRequest
        .newBuilder(URI.create("http://127.0.0.1:$port/devstoreaccount1?comp=list"))
        .timeout(Duration.ofSeconds(2))
        .build()
    waitFor(timeout = 30000, pollTime = 100) {
      try {
        client.send(request, HttpResponse.BodyHandlers.discarding())
        true
      } catch (_: IOException) {
        false
      }
    }
  }

  private fun docker(
    vararg args: String,
    timeoutMinutes: Long = 1,
  ): DockerResult {
    val outputFile = Files.createTempFile("docker-", ".log").toFile()
    try {
      val process =
        ProcessBuilder("docker", *args)
          .redirectErrorStream(true)
          .redirectOutput(outputFile)
          .start()
      if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
        process.destroyForcibly()
        throw IllegalStateException("docker ${args.joinToString(" ")} did not finish within $timeoutMinutes minute(s)")
      }
      return DockerResult(process.exitValue(), outputFile.readText())
    } finally {
      outputFile.delete()
    }
  }

  private class DockerResult(
    val exitValue: Int,
    val output: String,
  )
}
