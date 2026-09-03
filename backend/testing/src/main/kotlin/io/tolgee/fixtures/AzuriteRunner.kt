package io.tolgee.fixtures

import io.tolgee.misc.dockerRunner.DockerContainerRunner

class AzuriteRunner {
  companion object {
    val port: String = System.getenv("TOLGEE_TEST_AZURITE_PORT") ?: "51000"
    val containerName: String = System.getenv("TOLGEE_TEST_AZURITE_CONTAINER") ?: "server-integration-test-azurite"

    /**
     * Well-known Azurite development account, publicly documented by Microsoft. Not a secret.
     */
    val connectionString: String =
      "DefaultEndpointsProtocol=http;" +
        "AccountName=devstoreaccount1;" +
        "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
        "BlobEndpoint=http://127.0.0.1:$port/devstoreaccount1;"
  }

  private val runner =
    DockerContainerRunner(
      image = "mcr.microsoft.com/azure-storage/azurite:3.35.0",
      expose = mapOf(port to "10000"),
      waitForLog = "Azurite Blob service successfully listens on",
      rm = true,
      name = containerName,
      // The Azure SDK sends a newer x-ms-version than Azurite knows; without the flag every request is rejected.
      command = "azurite-blob --blobHost 0.0.0.0 --blobPort 10000 --skipApiVersionCheck --loose",
    )

  fun run() {
    runner.run()
  }

  fun stop() {
    runner.stop()
  }
}
