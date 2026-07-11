/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.findify.s3mock.S3Mock
import io.tolgee.configuration.tolgee.ContentStorageS3Properties
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.S3Exception

@ContextRecreatingTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileStorageS3Test : AbstractFileStorageServiceTest() {
  companion object {
    const val BUCKET_NAME = "testbucket"
  }

  @Autowired
  private lateinit var s3FileStorageFactory: S3FileStorageFactory
  lateinit var s3Mock: S3Mock

  val defaultProperties =
    ContentStorageS3Properties().apply {
      bucketName = BUCKET_NAME
      accessKey = "dummy_access_key"
      secretKey = "dummy_secret_key"
      endpoint = "http://localhost:29090"
      signingRegion = "dummy_signing_region"
    }

  val s3 by lazy {
    S3ClientProvider(
      defaultProperties,
    ).provide()
  }

  @BeforeAll
  fun setup() {
    s3Mock =
      S3Mock
        .Builder()
        .withPort(29090)
        .withInMemoryBackend()
        .build()
    s3Mock.start()
    s3.createBucket { req -> req.bucket(BUCKET_NAME) }
  }

  @AfterAll
  fun tearDown() {
    s3Mock.stop()
  }

  @Test
  fun testGetFile() {
    s3.putObject({ req -> req.bucket(BUCKET_NAME).key(testFilePath) }, RequestBody.fromString(testFileContent))
    val fileByteContent = testFileContent.toByteArray(charset("UTF-8"))
    assertThat(createFileStorage().readFile(testFilePath)).isEqualTo(fileByteContent)
  }

  @Test
  fun testDeleteFile() {
    s3.putObject({ req -> req.bucket(BUCKET_NAME).key(testFilePath) }, RequestBody.fromString(testFileContent))
    createFileStorage().deleteFile(testFilePath)
    assertThatExceptionOfType(S3Exception::class.java)
      .isThrownBy {
        s3.getObject { req ->
          req.bucket(BUCKET_NAME).key(testFilePath)
        }
      }
  }

  @Test
  fun testStoreFile() {
    createFileStorage().storeFile(testFilePath, testFileContent.toByteArray(charset("UTF-8")))
    assertThat(
      s3.getObject { req -> req.bucket(BUCKET_NAME).key(testFilePath) }.readAllBytes(),
    ).isEqualTo(testFileContent.toByteArray())
  }

  @Test
  fun testPruneDirectory() {
    createFileStorage().storeFile(testFilePath, testFileContent.toByteArray(charset("UTF-8")))
    createFileStorage().pruneDirectory("test")
    assertThat(createFileStorage().fileExists(testFilePath)).isEqualTo(false)
  }

  @Test
  fun testFileExists() {
    s3.putObject({ req -> req.bucket(BUCKET_NAME).key(testFilePath) }, RequestBody.fromString(testFileContent))
    assertThat(createFileStorage().fileExists(testFilePath)).isTrue
  }

  @Test
  fun `stores files to path by config`() {
    val storage = s3FileStorageFactory.create(defaultProperties.copy(path = "content/path"))
    storage.storeFile(
      testFilePath,
      testFileContent.toByteArray(charset("UTF-8")),
    )
    assertThat(
      s3.getObject { req -> req.bucket(BUCKET_NAME).key("content/path/$testFilePath") }.readAllBytes(),
    ).isEqualTo(testFileContent.toByteArray())
  }

  fun createFileStorage(): S3FileStorage {
    return s3FileStorageFactory.create(defaultProperties)
  }
}
