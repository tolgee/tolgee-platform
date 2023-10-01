package io.tolgee.model.cdn

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.MapsId
import javax.persistence.OneToOne
import javax.validation.constraints.NotBlank

@Entity()
class S3CdnConfig(
  @MapsId
  @JoinColumn(name = "cdn_storage_id")
  @OneToOne(fetch = FetchType.LAZY)
  var cdnStorage: CdnStorage,
) : S3Config {
  @Id
  @Column(name = "cdn_storage_id")
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
}
