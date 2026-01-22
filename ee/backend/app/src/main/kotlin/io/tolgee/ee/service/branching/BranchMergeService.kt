package io.tolgee.ee.service.branching

import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.ee.exceptions.BranchMergeConflictNotResolvedException
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.service.branching.merging.BranchMergeAnalyzer
import io.tolgee.ee.service.branching.merging.BranchMergeExecutor
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.KeyRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.util.Logging
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BranchMergeService(
  private val branchMergeRepository: BranchMergeRepository,
  private val branchMergeChangeRepository: BranchMergeChangeRepository,
  private val branchMergeAnalyzer: BranchMergeAnalyzer,
  private val branchMergeExecutor: BranchMergeExecutor,
  private val branchSnapshotService: BranchSnapshotService,
  private val branchMergeKeyCloneFactory: BranchMergeKeyCloneFactory,
  private val keyRepository: KeyRepository,
  @Lazy
  private val languageService: LanguageService,
) : Logging {
  @Transactional
  fun dryRun(branchMerge: BranchMerge) {
    val changes = branchMergeAnalyzer.compute(branchMerge)
    branchMerge.sourceRevision = branchMerge.sourceBranch.revision
    branchMerge.targetRevision = branchMerge.targetBranch.revision
    branchMerge.changes.clear()
    branchMerge.changes.addAll(changes)
  }

  @Transactional
  fun dryRun(
    sourceBranch: Branch,
    targetBranch: Branch,
  ): BranchMerge {
    val branchMerge =
      BranchMerge().apply {
        this.sourceBranch = sourceBranch
        this.targetBranch = targetBranch
        this.sourceRevision = sourceBranch.revision
        this.targetRevision = targetBranch.revision
      }
    dryRun(branchMerge)
    branchMergeRepository.save(branchMerge)
    return branchMerge
  }

  @Transactional
  fun refresh(branchMerge: BranchMerge) {
    val resolvedConflicts =
      branchMerge.changes
        .filter { it.change == BranchKeyMergeChangeType.CONFLICT && it.resolution != null }
        .associateBy { ConflictKey(it.sourceKey?.id, it.targetKey?.id) }

    dryRun(branchMerge)

    branchMerge.changes
      .filter { it.change == BranchKeyMergeChangeType.CONFLICT }
      .forEach { change ->
        resolvedConflicts[ConflictKey(change.sourceKey?.id, change.targetKey?.id)]?.resolution?.let { resolution ->
          change.resolution = resolution
        }
      }
  }

  private data class ConflictKey(
    val sourceKeyId: Long?,
    val targetKeyId: Long?,
  )

  fun applyMerge(merge: BranchMerge) {
    try {
      branchMergeExecutor.execute(merge)
    } catch (_: BranchMergeConflictNotResolvedException) {
      throw BadRequestException(Message.BRANCH_MERGE_CONFLICTS_NOT_RESOLVED)
    }
  }

  fun getMerges(
    projectId: Long,
    pageable: Pageable,
  ): Page<BranchMergeView> {
    return branchMergeRepository.findBranchMerges(projectId, pageable)
  }

  fun getMergeView(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView {
    return branchMergeRepository.findBranchMergeView(projectId, mergeId)
      ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
  }

  fun getConflicts(
    project: Project,
    mergeId: Long,
    pageable: Pageable,
    userId: Long,
  ): Page<BranchMergeConflictView> {
    val merge =
      this.findActiveMerge(project.id, mergeId)
        ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    val conflicts = branchMergeChangeRepository.findBranchMergeConflicts(project.id, mergeId, pageable)

    val languages =
      languageService.getLanguagesForTranslationsView(
        project.languages.map { it.tag }.toSet(),
        project.id,
        userId,
      )
    val allowedLanguageTags = languages.map { it.tag }.toSet()

    val keyIds = (conflicts.map { it.sourceBranchKeyId } + conflicts.map { it.targetBranchKeyId }).toSet()
    val keysById =
      if (keyIds.isEmpty()) {
        emptyMap()
      } else {
        keyRepository.findAllDetailedByIdIn(keyIds).associateBy { it.id }
      }

    conflicts.forEach { conflict ->
      conflict.sourceBranchKey = keysById[conflict.sourceBranchKeyId]!!
      conflict.targetBranchKey = keysById[conflict.targetBranchKeyId]!!
      conflict.allowedLanguageTags = allowedLanguageTags
    }
    this.enrichConflicts(conflicts, merge, allowedLanguageTags)
    return conflicts
  }

  fun getChanges(
    project: Project,
    mergeId: Long,
    type: BranchKeyMergeChangeType?,
    pageable: Pageable,
    userId: Long,
  ): Page<BranchMergeChangeView> {
    val merge =
      findActiveMerge(project.id, mergeId)
        ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    val changes = branchMergeChangeRepository.findBranchMergeChanges(project.id, mergeId, type, pageable)

    val languages =
      languageService.getLanguagesForTranslationsView(
        project.languages.map { it.tag }.toSet(),
        project.id,
        userId,
      )
    val allowedLanguageTags = languages.map { it.tag }.toSet()

    val sourceIds = changes.mapNotNull { it.sourceBranchKeyId }
    val targetIds = changes.mapNotNull { it.targetBranchKeyId }
    val keyIds = (sourceIds + targetIds).toSet()
    val keysById =
      if (keyIds.isEmpty()) {
        emptyMap()
      } else {
        keyRepository.findAllDetailedByIdIn(keyIds).associateBy { it.id }
      }

    changes.forEach { change ->
      change.sourceBranchKeyId?.let { id -> change.sourceBranchKey = keysById[id] }
      change.targetBranchKeyId?.let { id -> change.targetBranchKey = keysById[id] }
      change.allowedLanguageTags = allowedLanguageTags
    }
    this.enrichChanges(changes, merge, allowedLanguageTags)

    return changes
  }

  fun getConflict(
    projectId: Long,
    mergeId: Long,
    changeId: Long,
  ): BranchMergeChange {
    return branchMergeChangeRepository.findActiveMergeConflict(projectId, mergeId, changeId)
      ?: throw NotFoundException(Message.BRANCH_MERGE_CHANGE_NOT_FOUND)
  }

  fun resolveConflict(
    projectId: Long,
    mergeId: Long,
    changeId: Long,
    resolution: BranchKeyMergeResolutionType,
  ) {
    val change = getConflict(projectId, mergeId, changeId)
    change.resolution = resolution
  }

  fun resolveAllConflicts(
    projectId: Long,
    mergeId: Long,
    resolution: BranchKeyMergeResolutionType,
  ) {
    val merge = findActiveMerge(projectId, mergeId) ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    merge.changes
      .filter { it.change == BranchKeyMergeChangeType.CONFLICT }
      .forEach { it.resolution = resolution }
  }

  fun findActiveMerge(
    projectId: Long,
    mergeId: Long,
  ): BranchMerge? {
    return branchMergeRepository.findActiveMerge(projectId, mergeId)
  }

  fun deleteMerge(
    projectId: Long,
    mergeId: Long,
  ) {
    val merge =
      branchMergeRepository.findMerge(projectId, mergeId)
        ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    branchMergeRepository.delete(merge)
  }

  fun enrichConflicts(
    conflicts: Page<BranchMergeConflictView>,
    merge: BranchMerge,
    allowedLanguageTags: Set<String>,
  ) {
    val snapshotOriginalKeyIds = conflicts.map { it.targetBranchKeyId }.toSet()
    val snapshotByTargetKeyId =
      branchSnapshotService
        .getSnapshotKeysByOriginalKeyIdIn(merge.sourceBranch.id, snapshotOriginalKeyIds)
        .associateBy { it.originalKeyId }
    conflicts.forEach { conflict ->
      conflict.effectiveResolutionType = conflict.resolutionType
      conflict.changedTranslations =
        computeChangedTranslations(conflict.sourceBranchKey, conflict.targetBranchKey, allowedLanguageTags)
      val resolution = conflict.resolutionType ?: return@forEach
      val snapshot = snapshotByTargetKeyId[conflict.targetBranchKeyId]
      conflict.mergedBranchKey =
        mergeKeyView(conflict.sourceBranchKey, conflict.targetBranchKey, snapshot, resolution)
    }
  }

  fun enrichChanges(
    changes: Page<BranchMergeChangeView>,
    merge: BranchMerge,
    allowedLanguageTags: Set<String>,
  ) {
    val snapshotOriginalKeyIds = changes.mapNotNull { it.targetBranchKeyId }.toSet()
    val snapshotByTargetKeyId =
      branchSnapshotService
        .getSnapshotKeysByOriginalKeyIdIn(merge.sourceBranch.id, snapshotOriginalKeyIds)
        .associateBy { it.originalKeyId }
    changes.forEach { change ->
      change.effectiveResolutionType = change.resolutionType
      if (change.changeType == BranchKeyMergeChangeType.UPDATE ||
        change.changeType == BranchKeyMergeChangeType.CONFLICT
      ) {
        val sourceKey = change.sourceBranchKey
        val targetKey = change.targetBranchKey
        change.changedTranslations = computeChangedTranslations(sourceKey, targetKey, allowedLanguageTags)
        val resolution =
          when (change.changeType) {
            BranchKeyMergeChangeType.CONFLICT -> change.resolutionType
            else -> change.resolutionType ?: BranchKeyMergeResolutionType.SOURCE
          }
        if (sourceKey != null && targetKey != null && resolution != null) {
          val snapshot = change.targetBranchKeyId?.let { snapshotByTargetKeyId[it] }
          change.mergedBranchKey = mergeKeyView(sourceKey, targetKey, snapshot, resolution)
        }
      }
    }
  }

  private fun mergeKeyView(
    source: Key,
    target: Key,
    snapshot: KeySnapshot?,
    resolution: BranchKeyMergeResolutionType,
  ): Key {
    val merged = branchMergeKeyCloneFactory.cloneForMerge(target)
    merged.merge(source, snapshot, resolution)
    return merged
  }

  private fun computeChangedTranslations(
    source: Key?,
    target: Key?,
    allowedLanguageTags: Set<String>,
  ): List<String>? {
    if (source == null || target == null) {
      return null
    }
    val sourceByLang = source.translations.associateBy { it.language.tag }
    val targetByLang = target.translations.associateBy { it.language.tag }
    val languages = (sourceByLang.keys + targetByLang.keys).filter { it in allowedLanguageTags }
    return languages
      .filter { language ->
        Translation.differ(sourceByLang[language], targetByLang[language])
      }.sorted()
  }
}
