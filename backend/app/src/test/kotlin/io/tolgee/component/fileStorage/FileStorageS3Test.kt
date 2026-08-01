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
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
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

  private val testFileBytes = testFileContent.toByteArray(Charsets.UTF_8)

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
    assertThat(createFileStorage().readFile(testFilePath)).isEqualTo(testFileBytes)
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
    createFileStorage().storeFile(testFilePath, testFileBytes)
    assertThat(getStoredObject().readAllBytes()).isEqualTo(testFileBytes)
  }

  @Test
  fun `does not set a content type on the put request when none is given`() {
    val spiedS3 = Mockito.spy(s3)
    S3FileStorage(bucketName = BUCKET_NAME, path = null, s3 = spiedS3)
      .storeFile("test/content-type/none.txt", testFileBytes)

    val captor = argumentCaptor<PutObjectRequest>()
    verify(spiedS3).putObject(captor.capture(), any<RequestBody>())
    assertThat(captor.firstValue.contentType()).isNull()
  }

  @Test
  fun `stores file with content type`() {
    createFileStorage().storeFile(
      "test/content-type/json.txt",
      testFileBytes,
      "application/json",
    )
    val stored = getStoredObject("test/content-type/json.txt")
    assertThat(stored.readAllBytes()).isEqualTo(testFileBytes)
    assertThat(stored.response().contentType()).isEqualTo("application/json")
  }

  @Test
  fun `stores file with charset parameterised content type`() {
    createFileStorage().storeFile(
      "test/content-type/text.txt",
      testFileBytes,
      "text/plain; charset=UTF-8",
    )
    val stored = getStoredObject("test/content-type/text.txt")
    assertThat(stored.response().contentType()).isEqualTo("text/plain; charset=UTF-8")
  }

  @Test
  fun testPruneDirectory() {
    createFileStorage().storeFile(testFilePath, testFileBytes)
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
    storage.storeFile(testFilePath, testFileBytes)
    assertThat(
      s3.getObject { req -> req.bucket(BUCKET_NAME).key("content/path/$testFilePath") }.readAllBytes(),
    ).isEqualTo(testFileBytes)
  }

  private fun getStoredObject(key: String = testFilePath) = s3.getObject { req -> req.bucket(BUCKET_NAME).key(key) }

  fun createFileStorage(): S3FileStorage {
    return s3FileStorageFactory.create(defaultProperties)
  }
}
