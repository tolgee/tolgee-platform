package io.tolgee.model.cdn

interface S3Config : StorageConfig {
  val bucketName: String?
  val accessKey: String?
  val secretKey: String?
  val endpoint: String?
  val signingRegion: String?

  override val enabled: Boolean
    get() = bucketName != null

  override val cdnStorageType: CdnStorageType
    get() = CdnStorageType.S3
}
