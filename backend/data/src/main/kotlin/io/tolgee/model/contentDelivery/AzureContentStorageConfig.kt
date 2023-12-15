package io.tolgee.model.contentDelivery

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.validation.constraints.NotBlank

@Entity()
class AzureContentStorageConfig(
  @MapsId
  @JoinColumn(name = "content_storage_id")
  @OneToOne(fetch = FetchType.LAZY)
  var contentStorage: ContentStorage,
) : AzureBlobConfig {
  @Id
  @Column(name = "content_storage_id")
  private val id: Long? = null

  @field:NotBlank
  override var connectionString: String? = ""

  @field:NotBlank
  override var containerName: String? = ""
}
