package io.tolgee.service.projectExportImport

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.Project
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.projectExportImport.blob.BlobEntry
import io.tolgee.service.projectExportImport.blob.BlobHandler
import io.tolgee.service.projectExportImport.blob.BlobHandlerRegistry
import io.tolgee.service.projectExportImport.model.ExportManifest
import io.tolgee.service.projectExportImport.model.ExportZipLayout
import io.tolgee.service.projectExportImport.model.SerializedBigMeta
import jakarta.persistence.EntityManager
import jakarta.persistence.metamodel.EntityType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Serializes one project's OWNED subgraph and its external blobs into a self-contained export zip
 * (layout: [ExportZipLayout]). Rows are discovered per-OWNED-type via [ProjectScopedCollectorQueries]
 * and read reflectively by [EntityMetamodelReader]. The whole zip is built into a temp file inside one
 * read-only transaction so no DB/blob read happens lazily during streaming.
 */
@Component
class ProjectExportImportExporter(
  private val entityManager: EntityManager,
  private val objectMapper: ObjectMapper,
  private val blobHandlerRegistry: BlobHandlerRegistry,
  private val currentDateProvider: CurrentDateProvider,
  private val projectService: ProjectService,
  private val bigMetaService: BigMetaService,
) {
  /**
   * Builds the export zip in a temp file and returns it with the source project name (read in the same
   * transaction). The caller must delete the file.
   */
  @Transactional(readOnly = true)
  fun exportToTempFile(
    projectId: Long,
    version: String,
  ): ProjectExportFile {
    val project = projectService.get(projectId)
    val tempFile = Files.createTempFile("tolgee-project-export-", ".zip")
    try {
      ZipOutputStream(BufferedOutputStream(Files.newOutputStream(tempFile))).use { zip ->
        val blobRowsByHandler = LinkedHashMap<BlobHandler, List<Any>>()
        val counts = writeEntities(zip, projectId, blobRowsByHandler)
        writeProject(zip, project)
        val bigMetaCount = writeBigMeta(zip, projectId)
        writeBlobs(zip, blobRowsByHandler, project)
        writeManifest(zip, project, version, counts, bigMetaCount)
      }
    } catch (e: Throwable) {
      Files.deleteIfExists(tempFile)
      throw e
    }
    return ProjectExportFile(tempFile, project.name)
  }

  data class ProjectExportFile(
    val path: Path,
    val projectName: String,
  )

  private fun writeEntities(
    zip: ZipOutputStream,
    projectId: Long,
    blobRowsByHandler: MutableMap<BlobHandler, List<Any>>,
  ): Map<String, Int> {
    val counts = LinkedHashMap<String, Int>()
    ownedEntityTypes().forEach { entityType ->
      val simpleName = entityType.javaType.simpleName
      val rows = collect(entityType, projectId)
      writeJsonArray(zip, ExportZipLayout.entityPath(simpleName), rows) { EntityMetamodelReader.read(it, entityType) }
      counts[simpleName] = rows.size
      blobHandlerRegistry.handlerFor(entityType.javaType.name)?.let { blobRowsByHandler[it] = rows }
    }
    return counts
  }

  private fun <T> writeJsonArray(
    zip: ZipOutputStream,
    entryName: String,
    rows: Iterable<T>,
    toSerialized: (T) -> Any,
  ) {
    zip.putNextEntry(ZipEntry(entryName))
    val generator = objectMapper.factory.createGenerator(zip).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
    generator.writeStartArray()
    rows.forEach { objectMapper.writeValue(generator, toSerialized(it)) }
    generator.writeEndArray()
    // close() (not flush) returns Jackson's pooled buffer; safe because AUTO_CLOSE_TARGET is off, so
    // the underlying zip stream stays open.
    generator.close()
    zip.closeEntry()
  }

  /**
   * Writes the PROJECT_ROOT row's own scalar columns. The project is kept (not recreated) on import, but
   * mirror semantics overwrite its scalars from source, so they must travel in the zip — the project
   * is not an OWNED type and has no `entities/` file. Only attrs are consumed on import; its associations
   * stay the target's.
   */
  private fun writeProject(
    zip: ZipOutputStream,
    project: Project,
  ) {
    val projectType = entityManager.metamodel.entity(Project::class.java)
    zip.putNextEntry(ZipEntry(ExportZipLayout.PROJECT))
    zip.write(objectMapper.writeValueAsBytes(EntityMetamodelReader.read(project, projectType)))
    zip.closeEntry()
  }

  private fun writeBigMeta(
    zip: ZipOutputStream,
    projectId: Long,
  ): Int {
    val rows = bigMetaService.findAllForExport(projectId)
    writeJsonArray(zip, ExportZipLayout.BIG_META, rows) {
      SerializedBigMeta(it.key1Id, it.key2Id, it.distance, it.hits)
    }
    return rows.size
  }

  private fun writeBlobs(
    zip: ZipOutputStream,
    blobRowsByHandler: Map<BlobHandler, List<Any>>,
    project: Project,
  ) {
    val writtenNames = HashSet<String>()
    blobRowsByHandler.forEach { (handler, rows) ->
      rows.forEach { row -> handler.export(row).forEach { writeBlob(zip, it, writtenNames) } }
    }
    blobHandlerRegistry.handlerFor(Project::class.java.name)?.export(project)?.forEach {
      writeBlob(zip, it, writtenNames)
    }
  }

  private fun writeBlob(
    zip: ZipOutputStream,
    blob: BlobEntry,
    writtenNames: MutableSet<String>,
  ) {
    if (!writtenNames.add(blob.name)) return
    zip.putNextEntry(ZipEntry(ExportZipLayout.blobPath(blob.name)))
    zip.write(blob.bytes)
    zip.closeEntry()
  }

  private fun writeManifest(
    zip: ZipOutputStream,
    project: Project,
    version: String,
    counts: Map<String, Int>,
    bigMetaCount: Int,
  ) {
    val manifest =
      ExportManifest(
        schemaVersion = version,
        sourceProjectName = project.name,
        exportedAt = currentDateProvider.date.time,
        entityCounts = counts,
        bigMetaCount = bigMetaCount,
      )
    zip.putNextEntry(ZipEntry(ExportZipLayout.MANIFEST))
    zip.write(objectMapper.writeValueAsBytes(manifest))
    zip.closeEntry()
  }

  private fun collect(
    entityType: EntityType<*>,
    projectId: Long,
  ): List<Any> {
    val jpql =
      ProjectScopedCollectorQueries.queriesByClassName[entityType.javaType.name]
        ?: throw IllegalStateException("No project-scoped collector query for OWNED type ${entityType.javaType.name}")
    return entityManager
      .createQuery(jpql, entityType.javaType)
      .setParameter("projectId", projectId)
      .resultList
      .filterNotNull()
  }

  private fun ownedEntityTypes(): List<EntityType<*>> =
    entityManager.metamodel.entities
      .filter { ProjectExportImportPolicyRegistry.policyOf(it.javaType.name) == ExportImportPolicy.OWNED }
      .sortedBy { it.javaType.simpleName }
}
