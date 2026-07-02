package io.tolgee.service.projectExportImport.sidechannel

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.bigMeta.KeysDistanceDto
import io.tolgee.service.projectExportImport.model.ExportZipLayout
import io.tolgee.service.projectExportImport.model.SerializedBigMeta
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Round-trips BigMeta (`KeysDistance`). Canonicalization and both-orderings de-duplication of the
 * remapped pairs live in [BigMetaService.storeImportedDistances].
 */
@Component
class BigMetaSideChannelHandler(
  private val bigMetaService: BigMetaService,
  private val objectMapper: ObjectMapper,
) : SideChannelHandler {
  override val entityClass: KClass<*> = KeysDistance::class
  override val entryName: String = ExportZipLayout.BIG_META

  override fun collectForExport(projectId: Long): List<Any> =
    bigMetaService.findAllForExport(projectId).map { SerializedBigMeta(it.key1Id, it.key2Id, it.distance, it.hits) }

  override fun restore(
    json: ByteArray,
    context: SideChannelImportContext,
  ) {
    val records = objectMapper.readValue(json, object : TypeReference<List<SerializedBigMeta>>() {})
    val dtos =
      records.mapNotNull { record ->
        val key1 = context.keyIdBySourceId[record.key1Id] ?: return@mapNotNull null
        val key2 = context.keyIdBySourceId[record.key2Id] ?: return@mapNotNull null
        KeysDistanceDto(
          key1Id = key1,
          key2Id = key2,
          distance = record.distance,
          projectId = context.targetProjectId,
          hits = record.hits,
          stored = false,
        )
      }
    bigMetaService.storeImportedDistances(dtos)
  }
}
