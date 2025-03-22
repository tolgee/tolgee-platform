package io.tolgee.model.contentDelivery

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "content_storage",
  indexes = [
    Index(columnList = "project_id"),
  ],
)
@ActivityLoggedEntity
class ContentStorage : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  var project: Project? = null

  @field:NotBlank
  @field:Size(max = 100)
  @ActivityDescribingProp
  @ActivityLoggedProp
  var name: String = ""

  @field:Size(max = 255)
  @ActivityLoggedProp
  var publicUrlPrefix: String? = null

  @OneToOne(mappedBy = "contentStorage", optional = true, orphanRemoval = true, fetch = FetchType.LAZY)
  var azureContentStorageConfig: AzureContentStorageConfig? = null

  @OneToOne(mappedBy = "contentStorage", optional = true, orphanRemoval = true, fetch = FetchType.LAZY)
  var s3ContentStorageConfig: S3ContentStorageConfig? = null

  val storageConfig: StorageConfig?
    get() = configs.single { it != null }

  val configs: List<StorageConfig?>
    get() = listOf(azureContentStorageConfig, s3ContentStorageConfig)
}
