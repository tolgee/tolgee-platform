package io.tolgee.ee.service.branching

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.events.OnBranchDeleted
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.branching.BranchCopyService
import io.tolgee.service.branching.BranchService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
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
  private val translationViewDataProvider: TranslationViewDataProvider,
  private val languageService: LanguageService,
  private val authenticationFacade: AuthenticationFacade,
) : BranchService {
  override fun getBranches(projectId: Long, page: Pageable, search: String?, activeOnly: Boolean?): Page<Branch> {
    val branches = branchRepository.getAllProjectBranches(projectId, page, search, activeOnly)
    if (branches.isEmpty) {
      val defaultBranch = defaultBranchCreator.create(projectId)
      return PageImpl(listOf(defaultBranch))
    }
    return branches
  }

  override fun getActiveBranch(projectId: Long, branchId: Long): Branch {
    return branchRepository.findActiveByProjectIdAndId(projectId, branchId)
      ?: throw BadRequestException(Message.BRANCH_NOT_FOUND)
  }

  private fun getBranch(projectId: Long, branchId: Long): Branch {
    return branchRepository.findByProjectIdAndId(projectId, branchId)
      ?: throw BadRequestException(Message.BRANCH_NOT_FOUND)
  }

  @Transactional
  override fun createBranch(
    projectId: Long,
    name: String,
    originBranchId: Long,
    author: UserAccount,
  ): Branch {
    val originBranch =
      branchRepository.findActiveByProjectIdAndId(projectId, originBranchId) ?: throw BadRequestException(
        Message.ORIGIN_BRANCH_NOT_FOUND
      )

    if (originBranch.project.id != projectId) {
      throw BadRequestException(Message.ORIGIN_BRANCH_NOT_FOUND)
    }

    val branch = createBranch(projectId, name, author).also {
      it.originBranch = originBranch
      it.pending = true
    }
    branchRepository.save(branch)

    branchCopyService.copy(projectId, originBranch, branch)
    branchSnapshotService.createInitialSnapshot(projectId, originBranch, branch)

    branch.pending = false
    branchRepository.save(branch)

    return branch
  }

  private fun createBranch(projectId: Long, name: String, author: UserAccount): Branch {
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
  override fun deleteBranch(projectId: Long, branchId: Long) {
    val branch = getBranch(projectId, branchId)
    if (branch.isDefault) throw PermissionException(Message.CANNOT_DELETE_DEFAULT_BRANCH)
    softDeleteBranch(branch)
    applicationContext.publishEvent(OnBranchDeleted(branch))
  }

  @Transactional
  override fun dryRunMerge(projectId: Long, request: DryRunMergeBranchRequest): BranchMerge {
    val sourceBranch = getActiveBranch(projectId, request.sourceBranchId)
    val targetBranch = getActiveBranch(projectId, request.targetBranchId)
    return dryRunMerge(sourceBranch, targetBranch)
  }

  @Transactional
  fun dryRunMerge(sourceBranch: Branch, targetBranch: Branch): BranchMerge {
    return branchMergeService.dryRun(sourceBranch, targetBranch)
  }

  @Transactional
  override fun applyMerge(projectId: Long, mergeId: Long) {
    val merge = branchMergeService.findMerge(projectId, mergeId)
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
      archiveBranch(merge.sourceBranch)
    }
  }

  override fun getBranchMergeView(projectId: Long, mergeId: Long): BranchMergeView {
    return branchMergeService.getMergeView(projectId, mergeId)
  }

  override fun getBranchMerges(projectId: Long, pageable: Pageable): Page<BranchMergeView> {
    return branchMergeService.getMerges(projectId, pageable)
  }

  @Transactional
  override fun getBranchMergeConflicts(
    projectId: Long,
    branchMergeId: Long,
    pageable: Pageable
  ): Page<BranchMergeConflictView> {
    val project = entityManager.getReference(Project::class.java, projectId)
    val conflicts = branchMergeService.getConflicts(projectId, branchMergeId, pageable)
    val languages =
      languageService.getLanguagesForTranslationsView(
        project.languages.map { it.tag }.toSet(),
        project.id,
        authenticationFacade.authenticatedUser.id,
      )

    val sourceFilter = TranslationFilters().apply { filterKeyId = conflicts.map { it.sourceBranchKeyId }.toList() }
    val targetFilter = TranslationFilters().apply { filterKeyId = conflicts.map { it.targetBranchKeyId }.toList() }

    val sourceKeys = translationViewDataProvider
      .getData(projectId, languages, pageable, sourceFilter)
      .associateBy { it.keyId }

    val targetKeys = translationViewDataProvider
      .getData(projectId, languages, pageable, targetFilter)
      .associateBy { it.keyId }

    conflicts.forEach { conflict ->
      conflict.sourceBranchKey = sourceKeys[conflict.sourceBranchKeyId]!!
      conflict.targetBranchKey = targetKeys[conflict.targetBranchKeyId]!!
    }
    return conflicts
  }

  @Transactional
  override fun resolveConflict(projectId: Long, mergeId: Long, request: ResolveBranchMergeConflictRequest) {
    val conflict = branchMergeService.getConflict(projectId, mergeId, request.changeId)
    conflict.resolution = request.resolve
  }

  private fun archiveBranch(branch: Branch) {
    branch.archivedAt = currentDateProvider.date
  }

  private fun softDeleteBranch(branch: Branch) {
    archiveBranch(branch)
    branch.deletedAt = currentDateProvider.date
  }

  @Transactional
  override fun deleteMerge(projectId: Long, mergeId: Long) {
    branchMergeService.deleteMerge(projectId, mergeId)
  }
}
