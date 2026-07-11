package io.tolgee.model.contentDelivery

interface S3Config : StorageConfig {
  val bucketName: String?
  val accessKey: String?
  val secretKey: String?
  val endpoint: String?
  val signingRegion: String?

  /**
   * Specifies an optional subfolder structure within s3 bucket to which content will be stored
   */
  val path: String?

  override val enabled: Boolean
    get() = bucketName != null

  override val contentStorageType: ContentStorageType
    get() = ContentStorageType.S3
}
