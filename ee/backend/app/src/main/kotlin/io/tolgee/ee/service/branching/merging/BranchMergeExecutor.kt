package io.tolgee.ee.service.branching.merging

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.exceptions.BranchMergeConflictNotResolvedException
import io.tolgee.ee.service.branching.BranchSnapshotService
import io.tolgee.events.OnTranslationTextsModified
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Translation
import io.tolgee.repository.KeyRepository
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.LinkedHashMap

@Service
class BranchMergeExecutor(
  private val keyRepository: KeyRepository,
  private val keyMetaService: KeyMetaService,
  private val keyService: KeyService,
  private val translationService: TranslationService,
  private val currentDateProvider: CurrentDateProvider,
  private val branchSnapshotService: BranchSnapshotService,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  @Transactional
  fun execute(merge: BranchMerge) {
    attachKeysForMerge(merge)
    val snapshotKeys by lazy {
      branchSnapshotService.getSnapshotKeys(merge.sourceBranch.id).associateBy { it.originalKeyId }
    }
    val mergedTranslationIds = mutableListOf<Long>()
    merge.changes.forEach { change ->
      when (change.change) {
        BranchKeyMergeChangeType.ADD -> {
          applyAddition(change, merge.targetBranch, mergedTranslationIds)
        }

        BranchKeyMergeChangeType.UPDATE -> {
          change.withSnapshotKey(snapshotKeys) { snapshotKey ->
            applyUpdate(change, snapshotKey, mergedTranslationIds)
          }
        }

        BranchKeyMergeChangeType.DELETE -> {
          applyDeletion(change)
        }

        BranchKeyMergeChangeType.CONFLICT -> {
          change.withSnapshotKey(snapshotKeys) { snapshotKey ->
            applyConflict(change, snapshotKey, mergedTranslationIds)
          }
        }
      }
    }

    if (mergedTranslationIds.isNotEmpty()) {
      applicationEventPublisher.publishEvent(
        OnTranslationTextsModified(this, mergedTranslationIds, merge.targetBranch.project.id),
      )
    }

    merge.changes.clear()
    merge.mergedAt = currentDateProvider.date
  }

  private fun attachKeysForMerge(merge: BranchMerge) {
    val sourceIds = merge.changes.mapNotNull { it.sourceKey?.id }.toSet()
    val targetIds = merge.changes.mapNotNull { it.targetKey?.id }.toSet()
    if (sourceIds.isEmpty() && targetIds.isEmpty()) {
      return
    }

    val detailedKeysById =
      keyRepository.findAllDetailedByIdIn(sourceIds + targetIds).associateBy { it.id }
    if (detailedKeysById.isNotEmpty()) {
      keyRepository.findAllWithScreenshotsByIdIn(detailedKeysById.keys)
    }

    merge.changes.forEach { change ->
      change.sourceKey?.id?.let { id -> change.sourceKey = detailedKeysById[id] }
      change.targetKey?.id?.let { id -> change.targetKey = detailedKeysById[id] }
    }
  }

  private fun applyConflict(
    change: BranchMergeChange,
    keySnapshot: KeySnapshot,
    mergedTranslationIds: MutableList<Long>,
  ) {
    change.resolution ?: throw BranchMergeConflictNotResolvedException()
    applyUpdate(change, keySnapshot, mergedTranslationIds)
  }

  private fun applyUpdate(
    change: BranchMergeChange,
    snapshotKey: KeySnapshot,
    mergedTranslationIds: MutableList<Long>,
  ) {
    val resolution = change.resolution ?: BranchKeyMergeResolutionType.SOURCE
    val sourceKey = change.sourceKey ?: return
    val targetKey =
      change.targetKey ?: run {
        applyAddition(change, change.branchMerge.targetBranch, mergedTranslationIds)
        return
      }
    targetKey.merge(sourceKey, snapshotKey, resolution)
    persistAfterMerge(targetKey, mergedTranslationIds)
  }

  private fun applyAddition(
    change: BranchMergeChange,
    targetBranch: Branch,
    mergedTranslationIds: MutableList<Long>,
  ) {
    if (change.resolution != BranchKeyMergeResolutionType.SOURCE) {
      return
    }
    val sourceKey = change.sourceKey ?: return

    val newKey =
      Key().apply {
        name = sourceKey.name
        namespace = sourceKey.namespace
        isPlural = sourceKey.isPlural
        pluralArgName = sourceKey.pluralArgName
        project = targetBranch.project
        branch = targetBranch
      }
    keyRepository.save(newKey)

    sourceKey.keyMeta?.let { sourceMeta ->
      val newMeta = keyMetaService.getOrCreateForKey(newKey)
      newMeta.description = sourceMeta.description
      newMeta.custom = sourceMeta.custom?.let { LinkedHashMap(it) }
      newMeta.tags.clear()
      newMeta.tags.addAll(sourceMeta.tags)
    }

    sourceKey.translations.forEach { sourceTranslation ->
      val translation =
        Translation(
          text = sourceTranslation.text,
        ).apply {
          key = newKey
          language = sourceTranslation.language
          state = sourceTranslation.state
          auto = sourceTranslation.auto
          mtProvider = sourceTranslation.mtProvider
          outdated = sourceTranslation.outdated
          qaChecksStale = true
        }
      sourceTranslation.labels.forEach(translation::addLabel)
      translationService.save(translation)
      mergedTranslationIds.add(translation.id)
    }

    sourceKey.keyScreenshotReferences.forEach { reference ->
      val newReference =
        KeyScreenshotReference().apply {
          key = newKey
          screenshot = reference.screenshot
          positions = reference.positions?.toMutableList()
          originalText = reference.originalText
        }
      newKey.keyScreenshotReferences.add(newReference)
    }
  }

  private fun applyDeletion(change: BranchMergeChange) {
    if (change.resolution != BranchKeyMergeResolutionType.SOURCE) {
      return
    }
    val targetKey = change.targetKey ?: return
    change.targetKey = null
    keyService.hardDelete(targetKey.id)
  }

  private fun persistAfterMerge(
    key: Key,
    mergedTranslationIds: MutableList<Long>,
  ) {
    key.translations.forEach {
      if (it.id == 0L) {
        translationService.save(it)
      }
      if (it.qaChecksStale) {
        mergedTranslationIds.add(it.id)
      }
    }
  }

  private inline fun BranchMergeChange.withSnapshotKey(
    snapshotKeys: Map<Long, KeySnapshot>,
    block: (KeySnapshot) -> Unit,
  ) {
    val id = targetKey?.id ?: return
    val snapshotKey = snapshotKeys[id] ?: return
    block(snapshotKey)
  }
}
