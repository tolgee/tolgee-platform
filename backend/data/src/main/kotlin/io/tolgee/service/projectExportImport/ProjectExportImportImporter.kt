package io.tolgee.service.projectExportImport

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.ActivityHolder
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.service.AvatarService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.projectExportImport.blob.ProjectAvatarBlobHandler
import io.tolgee.service.projectExportImport.blob.ScreenshotBlobHandler
import io.tolgee.service.projectExportImport.model.SerializedEntity
import io.tolgee.service.projectExportImport.sidechannel.SideChannelHandlerRegistry
import io.tolgee.service.projectExportImport.sidechannel.SideChannelImportContext
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import io.tolgee.util.ImageConverter
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.InputStream

/**
 * Applies an export zip onto an existing target project with mirror semantics: the target's in-scope
 * content is wiped and replaced by the source graph, all in the caller-facing transaction so any failure
 * rolls back to the pre-import state.
 *
 * The whole source graph is held in heap and wired across two insert phases, so the import runs as one
 * synchronous, unchunked transaction and is bounded to projects that fit in a single request. The target
 * project must be quiescent during import — concurrent edits are not supported.
 *
 * With `ignoreVersion` the schema-version gate is skipped and the wipe proceeds against a possibly-drifted
 * schema, where an insert that succeeds can still commit subtly-wrong data that no rollback catches.
 */
@Component
class ProjectExportImportImporter(
  private val entityManager: EntityManager,
  private val objectMapper: ObjectMapper,
  private val zipReader: TransferZipReader,
  private val clearer: ProjectContentClearer,
  private val deserializer: EntityGraphDeserializer,
  private val writer: EntityMetamodelWriter,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
  private val screenshotService: ScreenshotService,
  private val avatarService: AvatarService,
  private val languageStatsService: LanguageStatsService,
  private val activityHolder: ActivityHolder,
  private val sideChannelHandlerRegistry: SideChannelHandlerRegistry,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
) : Logging {
  // Without rollbackFor, a checked exception thrown mid-insert would commit with the wipe already applied,
  // leaving the target permanently emptied (the default rule rolls back only on runtime exceptions).
  @Transactional(rollbackFor = [Exception::class])
  fun import(
    input: InputStream,
    targetProjectId: Long,
    importingAdminId: Long,
    runningVersion: String,
    ignoreVersion: Boolean = false,
  ) {
    // Every IOException reachable here is archive-derived (corrupt/truncated zip or deflate stream), so a
    // malformed archive maps to a 400 across all three exception types rather than an uncaught 500.
    val parsed =
      try {
        zipReader.read(input)
      } catch (e: IllegalArgumentException) {
        throw corruptArchive(e)
      } catch (e: JacksonException) {
        throw corruptArchive(e)
      } catch (e: IOException) {
        throw corruptArchive(e)
      }
    if (!ignoreVersion && parsed.manifest.schemaVersion != runningVersion) {
      throw BadRequestException(
        Message.PROJECT_IMPORT_VERSION_MISMATCH,
        listOf(parsed.manifest.schemaVersion, runningVersion),
      )
    }

    // Parse project.json before the wipe: a zip missing (or corrupting) it must be rejected while the
    // target is still intact, else the wipe leaves the target with its own stale scalars.
    val projectJson = parsed.projectJson ?: throw BadRequestException(Message.PROJECT_IMPORT_MISSING_PROJECT_JSON)
    val projectRecord = corruptArchiveOnParseError { objectMapper.readValue(projectJson, SerializedEntity::class.java) }

    // Leave auto-completion off until commit: it fires on commit, so re-enabling it before this method
    // returns emits a spurious revision. Its ThreadLocal is transaction-scoped, so leaving it false does
    // not leak.
    activityHolder.enableAutoCompletion = false

    clearer.clear(projectService.get(targetProjectId))

    // getReference, not find(): find() initializes the row now, and the project's @PostLoad seeds a fresh
    // default language when it sees none (true right after the wipe), polluting the mirror. The lazy proxy
    // initializes only when touched post-insert, once the source languages exist.
    val project = entityManager.getReference(Project::class.java, targetProjectId)
    val importingAdmin = userAccountService.get(importingAdminId)

    val result =
      corruptArchiveOnParseError {
        deserializer.deserialize(
          parsed.entityJsonByType,
          project,
          importingAdmin,
          userResolver = cachingUserResolver(),
          projectRecord = projectRecord,
        )
      }

    mirrorProjectScalars(project, projectRecord, result.maxImportedTaskNumber)
    // Re-seed the default PROJECT-type TM the clearer removed. After the scalar mirror, since
    // createProjectTm reads name/baseLanguage/organizationOwner.
    translationMemoryManagementService.createProjectTm(project)

    entityManager.flush()
    // Every fallible step (side-channel restore, all image conversion) runs before any blob is written to
    // the non-transactional FileStorage, so a rollback leaves no orphaned files. flush first so imported
    // keys exist in the DB before a side-channel handler's insert.
    restoreSideChannels(parsed.sideChannels, result.keyIdBySourceId, targetProjectId)
    val preparedScreenshots = prepareScreenshots(result.screenshotsBySourceId, parsed.blobs)
    restoreAvatar(project, parsed.blobs)
    writePreparedScreenshots(preparedScreenshots)
    refreshDerivedData(targetProjectId)
  }

  private fun corruptArchive(e: Exception) =
    BadRequestException(Message.PROJECT_IMPORT_CORRUPT_ARCHIVE, listOf(e.message ?: ""))

  private inline fun <T> corruptArchiveOnParseError(parse: () -> T): T =
    try {
      parse()
    } catch (e: JacksonException) {
      throw corruptArchive(e)
    }

  private fun cachingUserResolver(): (String) -> UserAccount? {
    val cache = HashMap<String, UserAccount?>()
    return { username -> cache.getOrPut(username.lowercase()) { userAccountService.findActive(username) } }
  }

  /**
   * Overwrites the kept project row's scalar columns from source, except [MIRROR_EXCLUDED_PROJECT_ATTRS].
   * `lastTaskNumber` is reconciled to the high-watermark of source and imported tasks.
   */
  private fun mirrorProjectScalars(
    project: Project,
    projectRecord: SerializedEntity?,
    maxImportedTaskNumber: Long,
  ) {
    projectRecord ?: return
    val mirrored = projectRecord.copy(attrs = projectRecord.attrs.filterKeys { it !in MIRROR_EXCLUDED_PROJECT_ATTRS })
    writer.setBasicAttrs(project, mirrored)
    val sourceLastTaskNumber = (projectRecord.attrs["lastTaskNumber"] as? Number)?.toLong() ?: 0L
    project.lastTaskNumber = maxOf(sourceLastTaskNumber, maxImportedTaskNumber)
  }

  private class PreparedScreenshot(
    val screenshot: Screenshot,
    val full: ByteArray,
    val middleSized: ByteArray,
    val thumbnail: ByteArray,
  )

  /**
   * Converts each imported screenshot's bytes (middle/thumbnail sizes regenerated from the full image)
   * without writing anything, so a bad image throws here before [writePreparedScreenshots] touches storage.
   */
  private fun prepareScreenshots(
    screenshotsBySourceId: Map<Long, Screenshot>,
    blobs: Map<String, ByteArray>,
  ): List<PreparedScreenshot> =
    screenshotsBySourceId.mapNotNull { (sourceId, screenshot) ->
      val name = ScreenshotBlobHandler.blobName(sourceId, screenshot.extension)
      val full = blobs[name]
      if (full == null) {
        logger.warn("Imported screenshot $sourceId has no blob '$name' in the zip; stored without an image")
        return@mapNotNull null
      }
      corruptArchiveOnImageError {
        val converter = ImageConverter(full.inputStream())
        PreparedScreenshot(
          screenshot,
          full,
          converter.getThumbnail(ScreenshotService.MIDDLE_SIZED_MAX_DIMENSION).toByteArray(),
          converter.getThumbnail(ScreenshotService.THUMBNAIL_MAX_DIMENSION).toByteArray(),
        )
      }
    }

  private fun writePreparedScreenshots(prepared: List<PreparedScreenshot>) {
    prepared.forEach { screenshotService.storeFiles(it.screenshot, it.full, it.middleSized, it.thumbnail) }
  }

  /**
   * Restores the project avatar under a fresh hash (the source hash is id-derived and would dangle);
   * a source with no avatar clears the target's. storeAvatarFiles converts before it writes, so a bad
   * avatar throws with nothing yet stored.
   */
  private fun restoreAvatar(
    project: Project,
    blobs: Map<String, ByteArray>,
  ) {
    val avatar = blobs.entries.firstOrNull { it.key.startsWith(ProjectAvatarBlobHandler.AVATAR_BLOB_DIR) }
    if (avatar == null) {
      project.avatarHash = null
      return
    }
    project.avatarHash =
      corruptArchiveOnImageError { avatarService.storeAvatarFiles(avatar.value.inputStream(), project) }
  }

  // ImageConverter.getThumbnail throws NPE, IIOException, or IllegalArgumentException on malformed image
  // bytes; map all to a corrupt-archive 400 rather than an uncaught 500.
  private inline fun <T> corruptArchiveOnImageError(convert: () -> T): T =
    try {
      convert()
    } catch (e: IOException) {
      throw corruptArchive(e)
    } catch (e: RuntimeException) {
      throw corruptArchive(e)
    }

  /** Restores every SIDE_CHANNEL entity from its own zip entry, remapping source ids to the imported rows. */
  private fun restoreSideChannels(
    sideChannels: Map<String, ByteArray>,
    keyIdBySourceId: Map<Long, Long>,
    targetProjectId: Long,
  ) {
    val context = SideChannelImportContext(targetProjectId, keyIdBySourceId)
    sideChannels.forEach { (entryName, json) ->
      val handler = sideChannelHandlerRegistry.byEntryName(entryName)
      if (handler == null) {
        logger.warn("Imported archive has side channel '$entryName' with no handler; skipping it")
        return@forEach
      }
      corruptArchiveOnParseError { handler.restore(json, context) }
    }
  }

  private fun refreshDerivedData(projectId: Long) {
    languageStatsService.refreshLanguageStatsAllBranches(projectId)
  }

  companion object {
    private val MIRROR_EXCLUDED_PROJECT_ATTRS =
      setOf("slug", "lastTaskNumber", "avatarHash", "deletedAt", "createdAt", "updatedAt")
  }
}
