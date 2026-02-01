package io.tolgee.ee.service.branching

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.RevisionType
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
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.branching.AbstractBranchService
import io.tolgee.service.branching.BranchCopyService
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Primary
@Service
class BranchServiceImpl(
  override val branchRepository: BranchRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
  private val branchCopyService: BranchCopyService,
  private val branchSnapshotService: BranchSnapshotService,
  private val applicationContext: ApplicationContext,
  private val branchMergeService: BranchMergeService,
  private val taskService: TaskService,
  private val authenticationFacade: AuthenticationFacade,
  private val projectBranchingMigrationService: ProjectBranchingMigrationService,
  private val activityHolder: ActivityHolder,
) : AbstractBranchService(branchRepository) {
  override fun getBranches(
    projectId: Long,
    page: Pageable,
    search: String?,
  ): Page<Branch> {
    return branchRepository.getAllProjectBranches(projectId, page, search)
  }

  private fun getActiveBranch(
    projectId: Long,
    branchId: Long,
  ): Branch {
    return branchRepository.findActiveByProjectIdAndId(projectId, branchId)
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

  @Transactional
  override fun setProtected(
    projectId: Long,
    branchId: Long,
    isProtected: Boolean,
  ): Branch {
    val branch = getActiveBranchWithMerge(projectId, branchId)
    branch.isProtected = isProtected
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
    activityHolder.forceEntityRevisionType(branch, RevisionType.DEL)
    softDeleteBranch(branch)
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
      branchMergeService.findActiveMergeFull(projectId, mergeId)
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
        softDeleteBranch(merge.sourceBranch)
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
      branchMergeService.findActiveMergeFull(projectId, mergeId)
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
    val user = authenticationFacade.authenticatedUser
    return branchMergeService.getConflicts(project, branchMergeId, pageable, user.id)
  }

  @Transactional
  override fun getBranchMergeChanges(
    projectId: Long,
    branchMergeId: Long,
    type: BranchKeyMergeChangeType?,
    pageable: Pageable,
  ): Page<BranchMergeChangeView> {
    val project = entityManager.getReference(Project::class.java, projectId)
    val user = authenticationFacade.authenticatedUser
    return branchMergeService.getChanges(project, branchMergeId, type, pageable, user.id)
  }

  @Transactional
  override fun getBranchMergeChange(
    projectId: Long,
    branchMergeId: Long,
    changeId: Long,
  ): BranchMergeChangeView {
    val project = entityManager.getReference(Project::class.java, projectId)
    val user = authenticationFacade.authenticatedUser
    return branchMergeService.getChange(project, branchMergeId, changeId, user.id)
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

  private fun softDeleteBranch(branch: Branch) {
    branch.deletedAt = currentDateProvider.date
    branch.lastMerge?.sourceBranch?.let {
      taskService.cancelUnfinishedTasksForBranch(branch.project.id, it.id)
    }
    applicationContext.publishEvent(OnBranchDeleted(branch))
  }

  @Transactional
  override fun deleteMerge(
    projectId: Long,
    mergeId: Long,
  ) {
    branchMergeService.deleteMerge(projectId, mergeId)
  }

  override fun enableBranchingOnProject(projectId: Long) {
    projectBranchingMigrationService.enableBranching(projectId)
  }
}
