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
 * Applies an export zip onto an existing target project with **mirror** semantics: wipe the target's
 * in-scope content, then re-insert the source graph. The whole thing runs in one transaction so a failure
 * rolls back to the pre-import state.
 *
 * The schema-version gate runs first and before any mutation — a mismatched export aborts with the target
 * untouched, UNLESS [import]'s `ignoreVersion` is set, in which case the equality check is skipped and the
 * wipe proceeds against a possibly-drifted schema — where an insert that *succeeds* can commit
 * subtly-wrong/partial data (the unbounded hazard of the bypass, which no transaction can catch).
 *
 * The remaining steps are: clear-in-place, two-phase insert, project scalar mirror, blob restore, then a
 * derived-data refresh.
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
  // rollbackFor = Exception: the wipe runs before the re-insert, and a parse/IO failure mid-insert is a
  // checked exception that the default (runtime-only) rollback rule would let commit — leaving the target
  // permanently wiped. Rolling back on any exception keeps clear()+insert a single all-or-nothing unit.
  @Transactional(rollbackFor = [Exception::class])
  fun import(
    input: InputStream,
    targetProjectId: Long,
    importingAdminId: Long,
    runningVersion: String,
    ignoreVersion: Boolean = false,
  ) {
    // A malformed/hostile archive surfaces as IllegalArgumentException (zipReader's own guards), JacksonException
    // (an unparseable manifest), or an IOException (a corrupt/truncated zip or deflate stream — ZipException,
    // EOFException, ...); translate all of them to a clean 400 so a bad client upload is not a 500 + Sentry
    // alert (this endpoint is admin-facing recovery). Every IOException reachable here is archive-derived.
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

    // project.json is part of the archive contract and the source of the scalar mirror. Reject a zip that
    // lacks it (or whose content is unparseable) BEFORE the wipe — otherwise the target is cleared and then
    // left with its own stale scalars. Its bytes are stored raw by zipReader, so parse them here.
    val projectJson = parsed.projectJson ?: throw BadRequestException(Message.PROJECT_IMPORT_MISSING_PROJECT_JSON)
    val projectRecord = corruptArchiveOnParseError { objectMapper.readValue(projectJson, SerializedEntity::class.java) }

    // Suppress activity logging for the whole transaction (this orchestrator owns it): the wipe + re-insert
    // must not emit revisions. Per-entity disableActivityLogging is also set in EntityMetamodelWriter.
    // Do NOT restore this before the method returns: auto-completion fires when THIS transaction commits, so
    // flipping it back on inside the method re-enables it before commit and emits a spurious revision (see the
    // `produces no activity revisions` test). The holder is request/transaction-scoped and its ThreadLocal is
    // cleared on transaction completion (ActivityHolderProvider), so leaving it false does not leak.
    activityHolder.enableAutoCompletion = false

    clearer.clear(projectService.get(targetProjectId))

    // clear() ends by clearing the persistence context, so re-fetch managed instances afterwards —
    // mutating a detached project (scalar mirror, avatar, base language) would never be flushed. Use a lazy
    // reference, NOT find(): find() initializes the row immediately, and the project's @PostLoad creates a
    // brand-new default language when it sees none (true right after the wipe), polluting the mirror. The
    // proxy only initializes once we touch it post-insert, by which point the source languages exist.
    val project = entityManager.getReference(Project::class.java, targetProjectId)
    val importingAdmin = userAccountService.get(importingAdminId)

    // deserialize parses the entity JSON blobs; a malformed one is a corrupt archive (400), and the wipe
    // already done here rolls back via @Transactional so the target is restored.
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
    // Re-seed the project's default PROJECT-type TM the clearer removed, exactly as project creation
    // does (ProjectCreationService.createProject). Runs after the scalar mirror so name/baseLanguage/
    // organizationOwner are set — the inputs createProjectTm reads.
    translationMemoryManagementService.createProjectTm(project)
    restoreScreenshots(result.screenshotsBySourceId, parsed.blobs)
    restoreAvatar(project, parsed.blobs)

    entityManager.flush()
    restoreSideChannels(parsed.sideChannels, result.keyIdBySourceId, targetProjectId)
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

  /** Resolves a source username to a live target user (case-insensitive, disabled excluded), caching hits. */
  private fun cachingUserResolver(): (String) -> UserAccount? {
    val cache = HashMap<String, UserAccount?>()
    return { username -> cache.getOrPut(username.lowercase()) { userAccountService.findActive(username) } }
  }

  /**
   * Overwrites the kept project row's own scalar columns from source. Its associations are left as
   * the target's (notably `organizationOwner`, guarded by the owner-exclusivity invariant). The carve-outs:
   * `slug` is instance-unique (keep target's), `avatarHash` is restored as a blob, `lastTaskNumber` is the
   * high-watermark reconciled below, and the audit timestamps stay import-time.
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

  /**
   * Re-stores each imported screenshot's bytes. The row was persisted in phase A, so its id and createdAt
   * (and thus the hash-derived path `f(id, createdAt)`) already exist. Restore lives here rather than in the
   * deserializer because it needs the parsed blobs, which the deserializer never sees. The full image is
   * stored byte-identically from the zip; the middle/thumbnail sizes are regenerated, never trusted from it.
   */
  private fun restoreScreenshots(
    screenshotsBySourceId: Map<Long, Screenshot>,
    blobs: Map<String, ByteArray>,
  ) {
    screenshotsBySourceId.forEach { (sourceId, screenshot) ->
      val name = ScreenshotBlobHandler.blobName(sourceId, screenshot.extension)
      val full = blobs[name]
      if (full == null) {
        logger.warn("Imported screenshot $sourceId has no blob '$name' in the zip; stored without an image")
        return@forEach
      }
      val converter = ImageConverter(full.inputStream())
      val middleSized = converter.getThumbnail(600).toByteArray()
      val thumbnail = converter.getThumbnail(200).toByteArray()
      screenshotService.storeFiles(screenshot, full, middleSized, thumbnail)
    }
  }

  /**
   * Restores the project avatar by re-storing the source bytes under a fresh hash (computed from the NEW
   * project id) and overwriting `avatarHash` — the source path/hash is id-derived and would dangle. A
   * source with no avatar clears the target's (mirror).
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
    project.avatarHash = avatarService.storeAvatarFiles(avatar.value.inputStream(), project)
  }

  /**
   * Restores every SIDE_CHANNEL entity (e.g. BigMeta) from its own zip entry, remapping source ids to the
   * imported rows. Runs after the flush so the imported keys are in the DB before a handler's insert.
   */
  private fun restoreSideChannels(
    sideChannels: Map<String, ByteArray>,
    keyIdBySourceId: Map<Long, Long>,
    targetProjectId: Long,
  ) {
    val context = SideChannelImportContext(targetProjectId, keyIdBySourceId)
    sideChannels.forEach { (entryName, json) ->
      val handler = sideChannelHandlerRegistry.byEntryName(entryName)
      if (handler == null) {
        // Forward-compat: a source instance may carry a side channel this instance has no handler for
        // (only reachable via ignoreVersion, since the schema-version gate otherwise blocks it). Drop it.
        logger.warn("Imported archive has side channel '$entryName' with no handler; skipping it")
        return@forEach
      }
      corruptArchiveOnParseError { handler.restore(json, context) }
    }
  }

  private fun refreshDerivedData(projectId: Long) {
    // Do NOT recompute QA issues here — they regenerate lazily.
    languageStatsService.refreshLanguageStatsAllBranches(projectId)
  }

  companion object {
    private val MIRROR_EXCLUDED_PROJECT_ATTRS =
      setOf("slug", "lastTaskNumber", "avatarHash", "deletedAt", "createdAt", "updatedAt")
  }
}
