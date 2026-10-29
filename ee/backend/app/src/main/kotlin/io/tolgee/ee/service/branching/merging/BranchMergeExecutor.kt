package io.tolgee.ee.service.branching.merging

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.exceptions.BranchMergeConflictNotResolvedException
import io.tolgee.ee.service.branching.AppliedChange
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
import io.tolgee.service.branching.chooseThreeWay
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
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
  private val entityManager: EntityManager,
) {
  @Transactional
  fun execute(merge: BranchMerge): BranchMerge {
    attachKeysForMerge(merge)
    val snapshotKeys by lazy { branchSnapshotService.getSnapshotKeys(merge.childBranch.id) }
    val byTargetKeyId by lazy { snapshotKeys.associateBy { merge.targetKeyIdOf(it) } }
    val bySourceKeyId by lazy { snapshotKeys.associateBy { merge.sourceKeyIdOf(it) } }
    val applied =
      applyChanges(merge) { change ->
        change.targetKey?.id?.let { byTargetKeyId[it] } ?: change.sourceKey?.id?.let { bySourceKeyId[it] }
      }
    entityManager.flush()
    branchSnapshotService.advanceSnapshot(merge, applied)
    return finalizeMerge(merge)
  }

  private fun applyChanges(
    merge: BranchMerge,
    snapshotKeyOf: (BranchMergeChange) -> KeySnapshot?,
  ): List<AppliedChange> {
    val (deletions, rest) = merge.changes.partition { it.change == BranchKeyMergeChangeType.DELETE }
    val (additions, edits) = rest.partition { it.change == BranchKeyMergeChangeType.ADD }
    val deleted = deletions.mapNotNull { applyChange(it, merge, snapshotKeyOf) }
    keyService.softDeleteMultiple(deleted.map { it.targetKey.id })
    val edited = edits.mapNotNull { applyChange(it, merge, snapshotKeyOf) }
    // Hibernate flushes inserts before updates; without this an added key reusing a name that a rename
    // just freed violates the active-key unique index.
    entityManager.flush()
    return deleted + edited + additions.mapNotNull { applyChange(it, merge, snapshotKeyOf) }
  }

  private fun applyChange(
    change: BranchMergeChange,
    merge: BranchMerge,
    snapshotKeyOf: (BranchMergeChange) -> KeySnapshot?,
  ): AppliedChange? {
    return when (change.change) {
      BranchKeyMergeChangeType.ADD -> applyAddition(change, merge.targetBranch)
      BranchKeyMergeChangeType.UPDATE -> applyUpdate(change, snapshotKeyOf(change))
      BranchKeyMergeChangeType.DELETE -> applyDeletion(change)
      BranchKeyMergeChangeType.CONFLICT -> applyConflict(change, snapshotKeyOf(change))
    }
  }

  private fun finalizeMerge(merge: BranchMerge): BranchMerge {
    merge.markMerged(currentDateProvider.date)
    return merge
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
    keySnapshot: KeySnapshot?,
  ): AppliedChange? {
    change.resolution ?: throw BranchMergeConflictNotResolvedException()
    return applyUpdate(change, keySnapshot)
  }

  private fun applyUpdate(
    change: BranchMergeChange,
    snapshotKey: KeySnapshot?,
  ): AppliedChange? {
    val resolution = change.resolution ?: BranchKeyMergeResolutionType.SOURCE
    val sourceKey = change.sourceKey ?: return null
    val targetKey =
      change.targetKey ?: return applyAddition(change, change.branchMerge.targetBranch)
    if (snapshotKey == null && resolution == BranchKeyMergeResolutionType.TARGET) {
      return AppliedChange(sourceKey, targetKey)
    }
    targetKey.merge(sourceKey, snapshotKey, resolution)
    val name = chooseThreeWay(sourceKey.name, targetKey.name, snapshotKey?.name, resolution) ?: targetKey.name
    val namespace =
      chooseThreeWay(sourceKey.namespace?.name, targetKey.namespace?.name, snapshotKey?.namespace, resolution)
    val targetBranch = change.branchMerge.targetBranch
    keyService.edit(targetKey, name, namespace, targetBranch.name.takeUnless { targetBranch.isDefault })
    persistAfterMerge(targetKey)
    return AppliedChange(sourceKey, targetKey)
  }

  private fun applyAddition(
    change: BranchMergeChange,
    targetBranch: Branch,
  ): AppliedChange? {
    if (change.resolution != BranchKeyMergeResolutionType.SOURCE) {
      return null
    }
    val sourceKey = change.sourceKey ?: return null

    val newKey =
      Key().apply {
        name = sourceKey.name
        namespace = sourceKey.namespace
        isPlural = sourceKey.isPlural
        pluralArgName = sourceKey.pluralArgName
        maxCharLimit = sourceKey.maxCharLimit
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
      sourceTranslation.labels.forEach(translation::addLabel)
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
    return AppliedChange(sourceKey, newKey)
  }

  private fun applyDeletion(change: BranchMergeChange): AppliedChange? {
    if (change.resolution != BranchKeyMergeResolutionType.SOURCE) {
      return null
    }
    val targetKey = change.targetKey ?: return null
    change.targetKey = null
    return AppliedChange(null, targetKey)
  }

  private fun persistAfterMerge(key: Key) {
    key.translations.forEach {
      if (it.id == 0L) {
        translationService.save(it)
      }
    }
  }
}
