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
class AzureContentStorageConfig : AzureConfig {
  @Id
  var id: Long = 0

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id")
  @MapsId
  var contentStorage: ContentStorage? = null

  @field:NotBlank
  override var connectionString: String = ""

  @field:NotBlank
  override var containerName: String = ""

  override val contentStorageType: ContentStorageType
    get() = ContentStorageType.AZURE
}
