package io.tolgee.hateoas.cdn

import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.ee.api.v2.hateoas.cdnStorage.CdnStorageModel
import io.tolgee.model.enums.TranslationState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "exporters", itemRelation = "exporter")
class CdnExporterModel(
  val id: Long,
  val name: String,
  val slug: String,
  val storage: CdnStorageModel?,
) : RepresentationModel<CdnExporterModel>(), Serializable, IExportParams {
  override var languages: Set<String>? = null
  override var format: ExportFormat = ExportFormat.JSON
  override var structureDelimiter: Char? = null
  override var filterKeyId: List<Long>? = null
  override var filterKeyIdNot: List<Long>? = null
  override var filterTag: String? = null
  override var filterKeyPrefix: String? = null
  override var filterState: List<TranslationState>? = null
  override var filterNamespace: List<String?>? = null
}
