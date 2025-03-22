package io.tolgee.model.contentDelivery

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.validation.constraints.NotBlank

@Entity
class S3ContentStorageConfig : S3Config {
  @Id
  var id: Long = 0

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id")
  @MapsId
  var contentStorage: ContentStorage? = null

  @field:NotBlank
  override var bucketName: String = ""

  @field:NotBlank
  override var accessKey: String = ""

  @field:NotBlank
  @Column(length = 1000)
  override var secretKey: String = ""

  @field:NotBlank
  override var endpoint: String = ""

  @field:NotBlank
  override var signingRegion: String = ""

  override val contentStorageType: ContentStorageType
    get() = ContentStorageType.S3
}
