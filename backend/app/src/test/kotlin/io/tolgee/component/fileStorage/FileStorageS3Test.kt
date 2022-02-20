/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import io.findify.s3mock.S3Mock
import io.tolgee.component.fileStorage.FileStorageS3Test.Companion.BUCKET_NAME
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.file-storage.s3.enabled=true",
    "tolgee.file-storage.s3.access-key=dummy_access_key",
    "tolgee.file-storage.s3.secret-key=dummy_secret_key",
    "tolgee.file-storage.s3.endpoint=http://localhost:29090",
    "tolgee.file-storage.s3.signing-region=dummy_signing_region",
    "tolgee.file-storage.s3.bucket-name=$BUCKET_NAME",
  ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileStorageS3Test : AbstractFileStorageServiceTest() {
  companion object {
    const val BUCKET_NAME = "testbucket"
  }

  @Autowired
  lateinit var s3: AmazonS3

  lateinit var s3Mock: S3Mock

  @BeforeAll
  fun setup() {
    s3Mock = S3Mock.Builder().withPort(29090).withInMemoryBackend().build()
    s3Mock.start()
    s3.createBucket(BUCKET_NAME)
  }

  @AfterAll
  fun tearDown() {
    s3Mock.stop()
  }

  @Test
  fun testGetFile() {
    s3.putObject(BUCKET_NAME, testFilePath, testFileContent)
    val fileByteContent = testFileContent.toByteArray(charset("UTF-8"))
    assertThat(fileStorage.readFile(testFilePath)).isEqualTo(fileByteContent)
  }

  @Test
  fun testDeleteFile() {
    s3.putObject(BUCKET_NAME, testFilePath, testFileContent)
    fileStorage.deleteFile(testFilePath)
    assertThatExceptionOfType(AmazonS3Exception::class.java).isThrownBy { s3.getObject(BUCKET_NAME, testFilePath) }
  }

  @Test
  fun testStoreFile() {
    fileStorage.storeFile(testFilePath, testFileContent.toByteArray(charset("UTF-8")))
    assertThat(
      s3.getObject(BUCKET_NAME, testFilePath).objectContent.readAllBytes()
    ).isEqualTo(testFileContent.toByteArray())
  }

  @Test
  fun testFileExists() {
    s3.putObject(BUCKET_NAME, testFilePath, testFileContent)
    assertThat(fileStorage.fileExists(testFilePath)).isTrue
  }
}
