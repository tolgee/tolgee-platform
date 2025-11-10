package io.tolgee.model.contentDelivery

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.validation.constraints.NotBlank

@Entity
class S3ContentStorageConfig(
  @MapsId
  @JoinColumn(name = "content_storage_id")
  @OneToOne(fetch = FetchType.LAZY)
  var contentStorage: ContentStorage,
) : S3Config {
  @Id
  @Column(name = "content_storage_id")
  private val id: Long? = null

  @field:NotBlank
  override var bucketName: String = ""

  @field:NotBlank
  override var accessKey: String = ""

  @field:NotBlank
  override var secretKey: String = ""

  @field:NotBlank
  override var endpoint: String = ""

  @field:NotBlank
  override var signingRegion: String = ""

  @field:Schema(
    description = "Specifies an optional subfolder structure within s3 bucket to which content will be stored",
  )
  override var path: String? = ""
}
