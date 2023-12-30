package io.tolgee.model.contentDelivery

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.enums.TranslationState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Type
import java.util.*

@Entity
@Table(
  uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "slug"])],
)
class ContentDeliveryConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel(), IExportParams {
  var name: String = ""

  var slug: String = ""

  @ManyToOne
  var contentStorage: ContentStorage? = null

  @OneToMany(mappedBy = "contentDeliveryConfig")
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  var lastPublished: Date? = null

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  override var languages: Set<String>? = null

  override var format: ExportFormat = ExportFormat.JSON
  override var structureDelimiter: Char? = '.'

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  override var filterKeyId: List<Long>? = null

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  override var filterKeyIdNot: List<Long>? = null
  override var filterTag: String? = null
  override var filterKeyPrefix: String? = null

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  override var filterState: List<TranslationState>? =
    listOf(
      TranslationState.TRANSLATED,
      TranslationState.REVIEWED,
    )

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  override var filterNamespace: List<String?>? = null
}
