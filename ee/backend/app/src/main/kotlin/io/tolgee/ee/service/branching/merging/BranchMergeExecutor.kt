package io.tolgee.ee.service.branching.merging

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.exceptions.BranchMergeConflictNotResolvedException
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.KeyRepository
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Service
import java.util.LinkedHashMap

@Service
class BranchMergeExecutor(
  private val keyRepository: KeyRepository,
  private val keyMetaService: KeyMetaService,
  private val keyService: KeyService,
  private val translationService: TranslationService,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun execute(merge: BranchMerge) {
    merge.changes.forEach { change ->
      when (change.change) {
        BranchKeyMergeChangeType.ADD -> applyAddition(change, merge.targetBranch)
        BranchKeyMergeChangeType.UPDATE -> applyUpdate(change)
        BranchKeyMergeChangeType.DELETE -> applyDeletion(change)
        BranchKeyMergeChangeType.CONFLICT -> applyConflict(change)
      }
    }
    merge.mergedAt = currentDateProvider.date
  }

  private fun applyConflict(change: BranchMergeChange) {
    when (change.resolution) {
      BranchKeyMergeResolutionType.SOURCE -> applyUpdate(change)
      BranchKeyMergeResolutionType.TARGET -> Unit
      null -> throw BranchMergeConflictNotResolvedException()
    }
  }

  private fun applyUpdate(change: BranchMergeChange) {
    if (change.resolution != BranchKeyMergeResolutionType.SOURCE) {
      return
    }
    val sourceKey = change.sourceKey ?: return
    val targetKey =
      change.targetKey ?: run {
        applyAddition(change, change.branchMerge.targetBranch)
        return
      }
    targetKey.merge(sourceKey)
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
  }

  private fun applyDeletion(change: BranchMergeChange) {
    if (change.resolution != BranchKeyMergeResolutionType.SOURCE) {
      return
    }
    val targetKey = change.targetKey ?: return
    change.targetKey = null
    keyService.delete(targetKey.id)
  }
}
