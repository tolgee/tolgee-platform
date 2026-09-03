package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner
import java.io.IOException
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

class AzuriteRunner {
  companion object {
    const val IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.37.0"

    val port: String = System.getenv("TOLGEE_TEST_AZURITE_PORT") ?: freePort()

    val containerName: String =
      System.getenv("TOLGEE_TEST_AZURITE_CONTAINER") ?: "server-integration-test-azurite-${UUID.randomUUID()}"

    /** Microsoft's published Azurite development account, so the key below is not a secret. */
    val connectionString: String =
      "DefaultEndpointsProtocol=http;" +
        "AccountName=devstoreaccount1;" +
        "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
        "BlobEndpoint=http://127.0.0.1:$port/devstoreaccount1;"

    private fun freePort(): String = ServerSocket(0).use { it.localPort.toString() }
  }

  private val runner =
    DockerContainerRunner(
      image = IMAGE,
      expose = mapOf(port to "10000"),
      waitForLog = "Azurite Blob service successfully listens on",
      rm = true,
      name = containerName,
      command = "azurite-blob --blobHost 0.0.0.0 --blobPort 10000",
    )

  fun run() {
    pullImage()
    runner.run()
    waitForBlobEndpoint()
  }

  fun stop() {
    runner.stop()
  }

  /**
   * A cold pull inside `docker run` can exceed DockerContainerRunner's command timeout on a slow CI runner.
   */
  private fun pullImage() {
    val process = ProcessBuilder("docker", "pull", IMAGE).redirectErrorStream(true).start()
    process.waitFor(10, TimeUnit.MINUTES)
    check(process.exitValue() == 0) { "docker pull $IMAGE failed:\n${process.inputStream.bufferedReader().readText()}" }
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
}
