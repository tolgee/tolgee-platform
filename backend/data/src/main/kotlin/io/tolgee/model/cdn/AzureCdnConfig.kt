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
class AzureCdnConfig(
  @MapsId
  @JoinColumn(name = "cdn_storage_id")
  @OneToOne(fetch = FetchType.LAZY)
  var cdnStorage: CdnStorage,
) : AzureBlobConfig {
  @Id
  @Column(name = "cdn_storage_id")
  private val id: Long? = null

  @field:NotBlank
  override var connectionString: String? = ""

  @field:NotBlank
  override var containerName: String? = ""
}
