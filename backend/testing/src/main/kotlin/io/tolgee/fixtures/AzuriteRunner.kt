package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

class AzuriteRunner {
  companion object {
    private const val CONTAINER_PORT = "10000"
    private const val ANY_FREE_HOST_PORT = "0"
    private val configuredPort: String? = System.getenv("TOLGEE_TEST_AZURITE_PORT")

    val containerName: String =
      System.getenv("TOLGEE_TEST_AZURITE_CONTAINER") ?: "server-integration-test-azurite-${UUID.randomUUID()}"
  }

  private val runner =
    DockerContainerRunner(
      image = "mcr.microsoft.com/azure-storage/azurite:3.37.0",
      expose = mapOf((configuredPort ?: ANY_FREE_HOST_PORT) to CONTAINER_PORT),
      waitForLog = "Azurite Blob service successfully listens on",
      rm = true,
      name = containerName,
      command = "azurite-blob --blobHost 0.0.0.0 --blobPort $CONTAINER_PORT",
    )

  lateinit var port: String
    private set

  /** Microsoft's published Azurite development account, so the key below is not a secret. */
  val connectionString: String
    get() =
      "DefaultEndpointsProtocol=http;" +
        "AccountName=devstoreaccount1;" +
        "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
        "BlobEndpoint=http://127.0.0.1:$port/devstoreaccount1;"

  fun run() {
    runner.run()
    port = configuredPort ?: runner.publishedPort(CONTAINER_PORT)
    waitForBlobEndpoint()
  }

  fun stop() {
    runner.stop()
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
