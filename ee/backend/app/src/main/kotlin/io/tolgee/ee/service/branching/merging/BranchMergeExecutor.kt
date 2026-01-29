package io.tolgee.ee.service.branching.merging

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.exceptions.BranchMergeConflictNotResolvedException
import io.tolgee.ee.service.branching.BranchSnapshotService
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
) {
  @Transactional
  fun execute(merge: BranchMerge) {
    attachKeysForMerge(merge)
    val snapshotKeys by lazy {
      branchSnapshotService.getSnapshotKeys(merge.sourceBranch.id).associateBy { it.originalKeyId }
    }
    merge.changes.forEach { change ->
      when (change.change) {
        BranchKeyMergeChangeType.ADD -> {
          applyAddition(change, merge.targetBranch)
        }

        BranchKeyMergeChangeType.UPDATE -> {
          change.withSnapshotKey(snapshotKeys) { snapshotKey ->
            applyUpdate(change, snapshotKey)
          }
        }

        BranchKeyMergeChangeType.DELETE -> {
          applyDeletion(change)
        }

        BranchKeyMergeChangeType.CONFLICT -> {
          change.withSnapshotKey(snapshotKeys) { snapshotKey ->
            applyConflict(change, snapshotKey)
          }
        }
      }
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
  ) {
    when (change.resolution) {
      BranchKeyMergeResolutionType.SOURCE -> applyUpdate(change, keySnapshot)
      BranchKeyMergeResolutionType.TARGET -> applyUpdate(change, keySnapshot)
      null -> throw BranchMergeConflictNotResolvedException()
    }
  }

  private fun applyUpdate(
    change: BranchMergeChange,
    snapshotKey: KeySnapshot,
  ) {
    val resolution = change.resolution ?: BranchKeyMergeResolutionType.SOURCE
    val sourceKey = change.sourceKey ?: return
    val targetKey =
      change.targetKey ?: run {
        applyAddition(change, change.branchMerge.targetBranch)
        return
      }
    targetKey.merge(sourceKey, snapshotKey, resolution)
    persistAfterMerge(targetKey)
  }

  private fun applyAddition(
    change: BranchMergeChange,
    targetBranch: Branch,
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
        }
      sourceTranslation.labels.forEach { label ->
        translation.addLabel(label)
      }
      translationService.save(translation)
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
    keyService.delete(targetKey.id)
  }

  private fun persistAfterMerge(key: Key) {
    key.translations.filter { it.id == 0L }.forEach { translationService.save(it) }
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
