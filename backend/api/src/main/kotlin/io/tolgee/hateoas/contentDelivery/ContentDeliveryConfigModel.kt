package io.tolgee.hateoas.contentDelivery

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.hateoas.ee.contentStorage.ContentStorageModel
import io.tolgee.model.enums.TranslationState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "contentDeliveryConfigs", itemRelation = "contentDeliveryConfig")
class ContentDeliveryConfigModel(
  val id: Long,
  val name: String,
  val slug: String,
  val pruneBeforePublish: Boolean,
  val storage: ContentStorageModel?,
  val publicUrl: String?,
  val autoPublish: Boolean,
  val lastPublished: Long?,
  val lastPublishedFiles: Collection<String>,
  override var escapeHtml: Boolean?,
) : RepresentationModel<ContentDeliveryConfigModel>(),
  Serializable,
  IExportParams {
  override var languages: Set<String>? = null
  override var format: ExportFormat = ExportFormat.JSON
  override var structureDelimiter: Char? = null
  override var filterKeyId: List<Long>? = null
  override var filterKeyIdNot: List<Long>? = null
  override var filterTag: String? = null
  override var filterTagIn: List<String>? = null
  override var filterTagNotIn: List<String>? = null
  override var filterKeyPrefix: String? = null
  override var filterState: List<TranslationState>? = null
  override var filterNamespace: List<String?>? = null
  override var messageFormat: ExportMessageFormat? = null
  override var supportArrays: Boolean = false
  override var fileStructureTemplate: String? = null
}
