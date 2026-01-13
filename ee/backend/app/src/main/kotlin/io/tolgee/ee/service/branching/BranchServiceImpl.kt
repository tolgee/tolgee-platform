package io.tolgee.ee.service.branching

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.dtos.request.branching.ResolveAllBranchMergeConflictsRequest
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.TaskService
import io.tolgee.events.OnBranchDeleted
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.repository.KeyRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.branching.BranchCopyService
import io.tolgee.service.branching.BranchService
import io.tolgee.service.language.LanguageService
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Primary
@Service
class BranchServiceImpl(
  private val branchRepository: BranchRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
  private val branchCopyService: BranchCopyService,
  private val branchSnapshotService: BranchSnapshotService,
  private val applicationContext: ApplicationContext,
  private val defaultBranchCreator: DefaultBranchCreator,
  private val branchMergeService: BranchMergeService,
  private val taskService: TaskService,
  private val languageService: LanguageService,
  private val keyRepository: KeyRepository,
  private val authenticationFacade: AuthenticationFacade,
) : BranchService {
  override fun getBranches(
    projectId: Long,
    page: Pageable,
    search: String?,
    activeOnly: Boolean?,
  ): Page<Branch> {
    val branches = branchRepository.getAllProjectBranches(projectId, page, search, activeOnly)
    if (branches.isEmpty) {
      val defaultBranch = defaultBranchCreator.create(projectId)
      return PageImpl(listOf(defaultBranch))
    }
    return branches
  }

  override fun getActiveBranch(
    projectId: Long,
    branchId: Long,
  ): Branch {
    return branchRepository.findActiveByProjectIdAndId(projectId, branchId)
      ?: throw NotFoundException(Message.BRANCH_NOT_FOUND)
  }

  override fun getActiveBranch(
    projectId: Long,
    branchName: String,
  ): Branch {
    return branchRepository.findActiveByProjectIdAndName(projectId, branchName)
      ?: throw NotFoundException(Message.BRANCH_NOT_FOUND)
  }

  private fun getActiveBranchWithMerge(
    projectId: Long,
    branchId: Long,
  ) = branchRepository.findActiveWithLatestMerge(projectId, branchId)
    ?: throw NotFoundException(Message.BRANCH_NOT_FOUND)

  @Transactional
  override fun renameBranch(
    projectId: Long,
    branchId: Long,
    name: String,
  ): Branch {
    val branch = getActiveBranchWithMerge(projectId, branchId)
    branchRepository.findActiveByProjectIdAndName(projectId, name)?.let {
      if (it.id != branchId) {
        throw BadRequestException(Message.BRANCH_ALREADY_EXISTS)
      }
    }
    branch.name = name
    branchRepository.save(branch)
    return branch
  }

  private fun getBranch(
    projectId: Long,
    branchId: Long,
  ): Branch {
    return branchRepository.findByProjectIdAndId(projectId, branchId)
      ?: throw NotFoundException(Message.BRANCH_NOT_FOUND)
  }

  @Transactional
  override fun createBranch(
    projectId: Long,
    name: String,
    originBranchId: Long,
    author: UserAccount,
  ): Branch {
    val originBranch =
      branchRepository.findActiveByProjectIdAndId(projectId, originBranchId) ?: throw NotFoundException(
        Message.ORIGIN_BRANCH_NOT_FOUND,
      )

    val branch =
      createBranch(projectId, name, author).also {
        it.originBranch = originBranch
        it.pending = true
      }
    branchRepository.save(branch)

    branchRepository.save(branch)
    branchCopyService.copy(projectId, originBranch, branch)
    branchSnapshotService.createInitialSnapshot(projectId, originBranch, branch)
    return branch
  }

  private fun createBranch(
    projectId: Long,
    name: String,
    author: UserAccount,
  ): Branch {
    branchRepository.findActiveByProjectIdAndName(projectId, name)?.let {
      throw BadRequestException(Message.BRANCH_ALREADY_EXISTS)
    }
    val project = entityManager.getReference(Project::class.java, projectId)

    return Branch().apply {
      this.project = project
      this.name = name
      this.author = author
    }
  }

  @Transactional
  override fun deleteBranch(
    projectId: Long,
    branchId: Long,
  ) {
    val branch = getBranch(projectId, branchId)
    if (branch.isDefault) throw PermissionException(Message.CANNOT_DELETE_DEFAULT_BRANCH)
    softDeleteBranch(branch)
    applicationContext.publishEvent(OnBranchDeleted(branch))
  }

  @Transactional
  override fun dryRunMerge(
    projectId: Long,
    request: DryRunMergeBranchRequest,
  ): BranchMerge {
    val sourceBranch = getActiveBranch(projectId, request.sourceBranchId)
    val origin = sourceBranch.originBranch ?: throw BadRequestException(Message.ORIGIN_BRANCH_NOT_FOUND)
    val targetBranch = getActiveBranch(projectId, origin.id)
    return dryRunMerge(sourceBranch, targetBranch)
  }

  @Transactional
  fun dryRunMerge(
    sourceBranch: Branch,
    targetBranch: Branch,
  ): BranchMerge {
    return branchMergeService.dryRun(sourceBranch, targetBranch)
  }

  @Transactional
  override fun applyMerge(
    projectId: Long,
    mergeId: Long,
    deleteBranch: Boolean?,
  ) {
    val merge =
      branchMergeService.findActiveMerge(projectId, mergeId)
        ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    if (!merge.isReadyToMerge) {
      if (!merge.isRevisionValid) {
        throw BadRequestException(Message.BRANCH_MERGE_REVISION_NOT_VALID)
      }
      if (!merge.isResolved) {
        throw BadRequestException(Message.BRANCH_MERGE_CONFLICTS_NOT_RESOLVED)
      }
    }
    branchMergeService.applyMerge(merge)
    if (!merge.sourceBranch.isDefault) {
      if (deleteBranch == true) {
        taskService.moveTasksAfterMerge(projectId, merge.sourceBranch, merge.targetBranch)
        archiveBranch(merge.sourceBranch)
      } else {
        branchSnapshotService.rebuildSnapshotFromSource(
          projectId = projectId,
          sourceBranch = merge.sourceBranch,
          targetBranch = merge.targetBranch,
        )
      }
    }
  }

  override fun getBranchMergeView(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView {
    return branchMergeService.getMergeView(projectId, mergeId)
  }

  @Transactional
  override fun refreshMerge(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView {
    val merge =
      branchMergeService.findActiveMerge(projectId, mergeId)
        ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    branchMergeService.refresh(merge)
    return branchMergeService.getMergeView(projectId, mergeId)
  }

  override fun getBranchMerges(
    projectId: Long,
    pageable: Pageable,
  ): Page<BranchMergeView> {
    return branchMergeService.getMerges(projectId, pageable)
  }

  @Transactional
  override fun getBranchMergeConflicts(
    projectId: Long,
    branchMergeId: Long,
    pageable: Pageable,
  ): Page<BranchMergeConflictView> {
    val project = entityManager.getReference(Project::class.java, projectId)
    val merge =
      branchMergeService.findActiveMerge(projectId, branchMergeId)
        ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    val conflicts = branchMergeService.getConflicts(projectId, branchMergeId, pageable)
    val languages =
      languageService.getLanguagesForTranslationsView(
        project.languages.map { it.tag }.toSet(),
        project.id,
        authenticationFacade.authenticatedUser.id,
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
    branchMergeService.enrichConflicts(conflicts, merge, allowedLanguageTags)
    return conflicts
  }

  @Transactional
  override fun getBranchMergeChanges(
    projectId: Long,
    branchMergeId: Long,
    type: BranchKeyMergeChangeType?,
    pageable: Pageable,
  ): Page<BranchMergeChangeView> {
    val project = entityManager.getReference(Project::class.java, projectId)
    val merge =
      branchMergeService.findActiveMerge(projectId, branchMergeId)
        ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    val changes = branchMergeService.getChanges(projectId, branchMergeId, type, pageable)

    val languages =
      languageService.getLanguagesForTranslationsView(
        project.languages.map { it.tag }.toSet(),
        project.id,
        authenticationFacade.authenticatedUser.id,
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
    branchMergeService.enrichChanges(changes, merge, allowedLanguageTags)

    return changes
  }

  @Transactional
  override fun resolveConflict(
    projectId: Long,
    mergeId: Long,
    request: ResolveBranchMergeConflictRequest,
  ) {
    branchMergeService.resolveConflict(projectId, mergeId, request.changeId, request.resolve)
  }

  @Transactional
  override fun resolveAllConflicts(
    projectId: Long,
    mergeId: Long,
    request: ResolveAllBranchMergeConflictsRequest,
  ) {
    branchMergeService.resolveAllConflicts(projectId, mergeId, request.resolve)
  }

  private fun archiveBranch(branch: Branch) {
    branch.archivedAt = currentDateProvider.date
    branch.lastMerge?.sourceBranch?.let {
      taskService.cancelUnfinishedTasksForBranch(branch.project.id, it.id)
    }
  }

  private fun softDeleteBranch(branch: Branch) {
    archiveBranch(branch)
    branch.deletedAt = currentDateProvider.date
  }

  @Transactional
  override fun deleteMerge(
    projectId: Long,
    mergeId: Long,
  ) {
    branchMergeService.deleteMerge(projectId, mergeId)
  }
}
