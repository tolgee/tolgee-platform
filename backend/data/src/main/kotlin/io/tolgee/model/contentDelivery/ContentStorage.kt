package io.tolgee.model.contentDelivery

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.validation.constraints.NotBlank

@Entity()
class ContentStorage(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,

  @NotBlank
  var name: String
) : StandardAuditModel() {

  var publicUrlPrefix: String? = null

  @OneToOne(mappedBy = "contentStorage", optional = true, orphanRemoval = true)
  var azureContentStorageConfig: AzureContentStorageConfig? = null

  @OneToOne(mappedBy = "contentStorage", optional = true, orphanRemoval = true)
  var s3ContentStorageConfig: S3ContentStorageConfig? = null

  val storageConfig: StorageConfig?
    get() = configs.single { it != null }

  val configs: List<StorageConfig?>
    get() = listOf(azureContentStorageConfig, s3ContentStorageConfig)
}
