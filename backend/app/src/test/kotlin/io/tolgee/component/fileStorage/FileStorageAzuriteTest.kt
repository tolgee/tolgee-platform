package io.tolgee.component.fileStorage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import io.tolgee.fixtures.AzuriteRunner
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.internal.use-in-memory-file-storage=false",
    "tolgee.file-storage.azure.enabled=true",
    "tolgee.file-storage.azure.container-name=${FileStorageAzuriteTest.CONTAINER_NAME}",
  ],
)
@ContextConfiguration(initializers = [FileStorageAzuriteTest.Companion.Initializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileStorageAzuriteTest : AbstractFileStorageServiceTest() {
  companion object {
    const val CONTAINER_NAME = "tolgee-test"
    val azuriteRunner = AzuriteRunner()

    val container: BlobContainerClient by lazy {
      BlobServiceClientBuilder()
        .connectionString(AzuriteRunner.connectionString)
        .buildClient()
        .getBlobContainerClient(CONTAINER_NAME)
    }

    /**
     * Beans write to the storage during context startup, so the container must exist before the context is built.
     */
    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        azuriteRunner.run()
        container.createIfNotExists()
        TestPropertyValues
          .of(mapOf("tolgee.file-storage.azure.connection-string" to AzuriteRunner.connectionString))
          .applyTo(configurableApplicationContext)
      }
    }
  }

  @AfterAll
  fun tearDown() {
    azuriteRunner.stop()
  }

  @BeforeEach
  fun cleanContainer() {
    container.listBlobs().forEach { container.getBlobClient(it.name).delete() }
  }

  @Test
  fun `is AzureBlobFileStorage`() {
    fileStorage.assert.isInstanceOf(AzureBlobFileStorage::class.java)
  }

  @Test
  fun testStoreFile() {
    fileStorage.storeFile(testFilePath, testFileContent.toByteArray(Charsets.UTF_8))
    container
      .getBlobClient(testFilePath)
      .downloadContent()
      .toString()
      .assert
      .isEqualTo(testFileContent)
  }

  @Test
  fun testReadFile() {
    uploadTestFile()
    fileStorage
      .readFile(testFilePath)
      .toString(Charsets.UTF_8)
      .assert
      .isEqualTo(testFileContent)
  }

  @Test
  fun testDeleteFile() {
    uploadTestFile()
    fileStorage.deleteFile(testFilePath)
    container
      .getBlobClient(testFilePath)
      .exists()
      .assert
      .isFalse()
  }

  @Test
  fun `deleteFile tolerates a missing blob`() {
    assertDoesNotThrow { fileStorage.deleteFile("not_existing") }
  }

  @Test
  fun testFileExists() {
    uploadTestFile()
    fileStorage.fileExists(testFilePath).assert.isTrue()
    fileStorage.fileExists("not_existing").assert.isFalse()
  }

  @Test
  fun testPruneDirectory() {
    val content = testFileContent.toByteArray(Charsets.UTF_8)
    fileStorage.storeFile("test/a.txt", content)
    fileStorage.storeFile("test/sub/b.txt", content)
    fileStorage.storeFile("other/c.txt", content)

    fileStorage.pruneDirectory("test")

    container
      .listBlobs()
      .map { it.name }
      .assert
      .containsExactly("other/c.txt")
  }

  private fun uploadTestFile() {
    container.getBlobClient(testFilePath).upload(BinaryData.fromString(testFileContent), true)
  }
}
