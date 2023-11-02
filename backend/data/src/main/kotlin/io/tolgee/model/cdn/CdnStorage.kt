package io.tolgee.model.cdn

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.validation.constraints.NotBlank

@Entity()
class CdnStorage(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,

  @NotBlank
  var name: String
) : StandardAuditModel() {

  var publicUrlPrefix: String? = null

  @OneToOne(mappedBy = "cdnStorage", fetch = FetchType.LAZY, optional = true)
  var azureCdnConfig: AzureCdnConfig? = null

  @OneToOne(mappedBy = "cdnStorage", fetch = FetchType.LAZY, optional = true)
  var s3CdnConfig: S3CdnConfig? = null

  val storageConfig: StorageConfig?
    get() = configs.single { it != null }

  val configs: List<StorageConfig?>
    get() = listOf(azureCdnConfig, s3CdnConfig)
}
