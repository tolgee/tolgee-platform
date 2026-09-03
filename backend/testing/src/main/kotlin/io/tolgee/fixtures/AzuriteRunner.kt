package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AzuriteRunner {
  companion object {
    val port: String = System.getenv("TOLGEE_TEST_AZURITE_PORT") ?: "51000"
    val containerName: String = System.getenv("TOLGEE_TEST_AZURITE_CONTAINER") ?: "server-integration-test-azurite"

    /** Microsoft's published Azurite development account, so the key below is not a secret. */
    val connectionString: String =
      "DefaultEndpointsProtocol=http;" +
        "AccountName=devstoreaccount1;" +
        "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
        "BlobEndpoint=http://127.0.0.1:$port/devstoreaccount1;"
  }

  private val runner =
    DockerContainerRunner(
      image = "mcr.microsoft.com/azure-storage/azurite:3.37.0",
      expose = mapOf(port to "10000"),
      waitForLog = "Azurite Blob service successfully listens on",
      rm = true,
      name = containerName,
      command = "azurite-blob --blobHost 0.0.0.0 --blobPort 10000",
    )

  fun run() {
    runner.run()
    waitForBlobEndpoint()
  }

  fun stop() {
    runner.stop()
  }

  /**
   * The container log says "listening" before the published port accepts connections on Docker Desktop.
   */
  private fun waitForBlobEndpoint() {
    val client = HttpClient.newHttpClient()
    val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/devstoreaccount1?comp=list")).build()
    waitFor(timeout = 30000, pollTime = 100) {
      try {
        client.send(request, HttpResponse.BodyHandlers.discarding())
        true
      } catch (e: IOException) {
        false
      }
    }
  }
}
