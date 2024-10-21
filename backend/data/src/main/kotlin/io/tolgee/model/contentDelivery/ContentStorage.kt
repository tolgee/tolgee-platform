package io.tolgee.model.contentDelivery

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "project_id"),
  ],
)
class ContentStorage(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
  @NotBlank
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String,
) : StandardAuditModel() {
  @ActivityLoggedProp
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
