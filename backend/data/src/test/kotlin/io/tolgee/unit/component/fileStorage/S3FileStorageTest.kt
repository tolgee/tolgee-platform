package io.tolgee.unit.component.fileStorage

import io.tolgee.component.fileStorage.S3FileStorage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class S3FileStorageTest {
  @Test
  fun `uploads files as public-read when publicRead is on`() {
    assertThat(storeAndCapture(publicRead = true).acl()).isEqualTo(ObjectCannedACL.PUBLIC_READ)
  }

  @Test
  fun `uploads files without ACL when publicRead is off`() {
    assertThat(storeAndCapture(publicRead = false).acl()).isNull()
  }

  private fun storeAndCapture(publicRead: Boolean): PutObjectRequest {
    val s3 = mock<S3Client>()
    S3FileStorage(bucketName = "bucket", path = "i18n", s3 = s3, publicRead = publicRead)
      .storeFile("en.json", "{}".toByteArray())

    val captor = argumentCaptor<PutObjectRequest>()
    verify(s3).putObject(captor.capture(), any<RequestBody>())
    assertThat(captor.firstValue.bucket()).isEqualTo("bucket")
    assertThat(captor.firstValue.key()).isEqualTo("i18n/en.json")
    return captor.firstValue
  }
}
