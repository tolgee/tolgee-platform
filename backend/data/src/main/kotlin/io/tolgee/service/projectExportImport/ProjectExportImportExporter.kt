package io.tolgee.service.projectExportImport

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.Project
import io.tolgee.service.project.ProjectService
import io.tolgee.service.projectExportImport.blob.BlobEntry
import io.tolgee.service.projectExportImport.blob.BlobHandler
import io.tolgee.service.projectExportImport.blob.BlobHandlerRegistry
import io.tolgee.service.projectExportImport.model.ExportManifest
import io.tolgee.service.projectExportImport.model.ExportZipLayout
import io.tolgee.service.projectExportImport.sidechannel.SideChannelHandlerRegistry
import jakarta.persistence.EntityManager
import jakarta.persistence.metamodel.EntityType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Serializes one project's OWNED subgraph and its external blobs into a self-contained export zip
 * (layout: [ExportZipLayout]). Rows are discovered per-OWNED-type via [ProjectScopedCollectorQueries] and
 * read reflectively by [EntityMetamodelReader].
 *
 * REPEATABLE READ so the per-type collector queries read one stable snapshot: under READ COMMITTED a
 * concurrent commit between two queries could tear the graph (e.g. a Translation whose Key was missed),
 * producing a zip that fails to import.
 */
@Component
class ProjectExportImportExporter(
  private val entityManager: EntityManager,
  private val objectMapper: ObjectMapper,
  private val blobHandlerRegistry: BlobHandlerRegistry,
  private val currentDateProvider: CurrentDateProvider,
  private val projectService: ProjectService,
  private val sideChannelHandlerRegistry: SideChannelHandlerRegistry,
) {
  /**
   * Builds the export zip in a temp file and returns it with the source project name (read in the same
   * transaction). The caller must delete the file.
   */
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
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
        val sideChannelCounts = writeSideChannels(zip, projectId)
        writeBlobs(zip, blobRowsByHandler, project)
        writeManifest(zip, project, version, counts, sideChannelCounts)
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
    // close(), not flush(), returns Jackson's pooled buffer; AUTO_CLOSE_TARGET is off so the zip stays open.
    generator.close()
    zip.closeEntry()
  }

  /**
   * Writes the PROJECT_ROOT row's own scalar columns to `project.json`. The project is kept (not an OWNED
   * type, no `entities/` file), but mirror semantics overwrite its scalars from source on import.
   */
  private fun writeProject(
    zip: ZipOutputStream,
    project: Project,
  ) {
    val projectType = entityManager.metamodel.entity(Project::class.java)
    val bytes = objectMapper.writeValueAsBytes(EntityMetamodelReader.read(project, projectType))
    writeRawEntry(zip, ExportZipLayout.PROJECT, bytes)
  }

  private fun writeRawEntry(
    zip: ZipOutputStream,
    entryName: String,
    bytes: ByteArray,
  ) {
    zip.putNextEntry(ZipEntry(entryName))
    zip.write(bytes)
    zip.closeEntry()
  }

  private fun writeSideChannels(
    zip: ZipOutputStream,
    projectId: Long,
  ): Map<String, Int> {
    val counts = LinkedHashMap<String, Int>()
    sideChannelHandlerRegistry.handlersInWriteOrder.forEach { handler ->
      val rows = handler.collectForExport(projectId)
      writeJsonArray(zip, handler.entryName, rows) { it }
      counts[handler.entryName] = rows.size
    }
    return counts
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
    writeRawEntry(zip, ExportZipLayout.blobPath(blob.name), blob.bytes)
  }

  private fun writeManifest(
    zip: ZipOutputStream,
    project: Project,
    version: String,
    counts: Map<String, Int>,
    sideChannelCounts: Map<String, Int>,
  ) {
    val manifest =
      ExportManifest(
        schemaVersion = version,
        sourceProjectName = project.name,
        exportedAt = currentDateProvider.date.time,
        entityCounts = counts,
        sideChannelCounts = sideChannelCounts,
      )
    writeRawEntry(zip, ExportZipLayout.MANIFEST, objectMapper.writeValueAsBytes(manifest))
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
