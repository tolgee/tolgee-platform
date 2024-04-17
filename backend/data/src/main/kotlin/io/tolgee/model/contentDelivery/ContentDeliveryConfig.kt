package io.tolgee.model.contentDelivery

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityIgnoredProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.enums.TranslationState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import java.util.*

@Entity
@Table(
  uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "slug"])],
)
@ActivityLoggedEntity
class ContentDeliveryConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel(), IExportParams {
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String = ""

  var slug: String = ""

  @ManyToOne
  var contentStorage: ContentStorage? = null

  @OneToMany(mappedBy = "contentDeliveryConfig")
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  @ActivityIgnoredProp
  var lastPublished: Date? = null

  @ColumnDefault("false")
  var pruneBeforePublish = true

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  @ActivityLoggedProp
  override var languages: Set<String>? = null

  @ActivityLoggedProp
  override var format: ExportFormat = ExportFormat.JSON

  @ActivityLoggedProp
  override var structureDelimiter: Char? = '.'

  @ColumnDefault("false")
  @ActivityLoggedProp
  override var supportArrays: Boolean = false

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  @ActivityLoggedProp
  override var filterKeyId: List<Long>? = null

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  @ActivityLoggedProp
  override var filterKeyIdNot: List<Long>? = null

  @ActivityLoggedProp
  override var filterTag: String? = null

  @ActivityLoggedProp
  override var filterKeyPrefix: String? = null

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  @ActivityLoggedProp
  override var filterState: List<TranslationState>? =
    listOf(
      TranslationState.TRANSLATED,
      TranslationState.REVIEWED,
    )

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  @ActivityLoggedProp
  override var filterNamespace: List<String?>? = null

  @Enumerated(EnumType.STRING)
  @ActivityLoggedProp
  override var messageFormat: ExportMessageFormat? = null
}
