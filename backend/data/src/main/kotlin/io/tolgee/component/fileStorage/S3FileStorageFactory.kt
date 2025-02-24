package io.tolgee.component.fileStorage

import io.tolgee.model.contentDelivery.S3Config
import org.springframework.stereotype.Component

@Component
class S3FileStorageFactory {
  fun create(config: S3Config): S3FileStorage {
    val client = S3ClientProvider(config).provide()
    val bucketName = config.bucketName ?: throw RuntimeException("Bucket name for S3 storage is not set")
    val path = getCanonicalPath(config.path)

    return S3FileStorage(bucketName = bucketName, path = path, s3 = client)
  }

  private fun getCanonicalPath(path: String?): String {
    return path?.let {
      if (it.isBlank()) {
        it.trim()
      } else if (it.endsWith("/")) {
        it
      } else {
        "$it/"
      }
    } ?: ""
  }
}
