/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.findify.s3mock.S3Mock
import io.tolgee.component.fileStorage.FileStorageS3Test.Companion.BUCKET_NAME
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.S3Exception

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.authentication.jwt-secret=this is a predefined secret, so it doesn't attempt to " +
      "get it from the S3 mock that doesn't exist yet :D",
    "tolgee.file-storage.s3.enabled=true",
    "tolgee.file-storage.s3.access-key=dummy_access_key",
    "tolgee.file-storage.s3.secret-key=dummy_secret_key",
    "tolgee.file-storage.s3.endpoint=http://localhost:29090",
    "tolgee.file-storage.s3.signing-region=dummy_signing_region",
    "tolgee.file-storage.s3.bucket-name=$BUCKET_NAME",
    "tolgee.authentication.initial-password=hey password manager, please don't use the filesystem :3",
    "tolgee.internal.use-in-memory-file-storage=false",
  ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileStorageS3Test : AbstractFileStorageServiceTest() {
  companion object {
    const val BUCKET_NAME = "testbucket"
  }

  lateinit var s3Mock: S3Mock

  val s3 by lazy {
    S3ClientProvider(tolgeeProperties.fileStorage.s3).provide()
  }

  @BeforeAll
  fun setup() {
    s3Mock = S3Mock.Builder().withPort(29090).withInMemoryBackend().build()
    s3Mock.start()
    s3.createBucket { req -> req.bucket(BUCKET_NAME) }
  }

  @AfterAll
  fun tearDown() {
    s3Mock.stop()
  }

  @Test
  fun `is S3FileStorage`() {
    assertThat(fileStorage is S3FileStorage).isTrue()
  }

  @Test
  fun testGetFile() {
    s3.putObject({ req -> req.bucket(BUCKET_NAME).key(testFilePath) }, RequestBody.fromString(testFileContent))
    val fileByteContent = testFileContent.toByteArray(charset("UTF-8"))
    assertThat(fileStorage.readFile(testFilePath)).isEqualTo(fileByteContent)
  }

  @Test
  fun testDeleteFile() {
    s3.putObject({ req -> req.bucket(BUCKET_NAME).key(testFilePath) }, RequestBody.fromString(testFileContent))
    fileStorage.deleteFile(testFilePath)
    assertThatExceptionOfType(S3Exception::class.java)
      .isThrownBy {
        s3.getObject { req ->
          req.bucket(BUCKET_NAME).key(testFilePath)
        }
      }
  }

  @Test
  fun testStoreFile() {
    fileStorage.storeFile(testFilePath, testFileContent.toByteArray(charset("UTF-8")))
    assertThat(
      s3.getObject { req -> req.bucket(BUCKET_NAME).key(testFilePath) }.readAllBytes(),
    ).isEqualTo(testFileContent.toByteArray())
  }

  @Test
  fun testPruneDirectory() {
    fileStorage.storeFile(testFilePath, testFileContent.toByteArray(charset("UTF-8")))
    fileStorage.pruneDirectory("test")
    assertThat(fileStorage.fileExists(testFilePath)).isEqualTo(false)
  }

  @Test
  fun testFileExists() {
    s3.putObject({ req -> req.bucket(BUCKET_NAME).key(testFilePath) }, RequestBody.fromString(testFileContent))
    assertThat(fileStorage.fileExists(testFilePath)).isTrue
  }
}
