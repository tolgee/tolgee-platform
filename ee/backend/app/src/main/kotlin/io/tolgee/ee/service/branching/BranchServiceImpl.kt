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
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeResolutionType
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
  private val applicationContext: ApplicationContext,
  private val defaultBranchCreator: DefaultBranchCreator,
  private val branchMergeService: BranchMergeService,
  private val translationViewDataProvider: TranslationViewDataProvider,
  private val languageService: LanguageService,
  private val authenticationFacade: AuthenticationFacade,
) : BranchService {
  override fun getAllBranches(projectId: Long, page: Pageable, search: String?): Page<Branch> {
    val branches = branchRepository.getAllProjectBranches(projectId, page, search)
    if (branches.isEmpty) {
      val defaultBranch = defaultBranchCreator.create(projectId)
      return PageImpl(listOf(defaultBranch))
    }
    return branches
  }

  override fun getBranch(projectId: Long, branchId: Long): Branch {
    return branchRepository.findByProjectIdAndId(projectId, branchId)
      ?: throw BadRequestException(Message.BRANCH_NOT_FOUND)
  }

  @Transactional
  override fun createBranch(projectId: Long, name: String, originBranchId: Long): Branch {
    val originBranch = branchRepository.findByProjectIdAndId(projectId, originBranchId) ?: throw BadRequestException(
      Message.ORIGIN_BRANCH_NOT_FOUND
    )

    if (originBranch.project.id != projectId) {
      throw BadRequestException(Message.ORIGIN_BRANCH_NOT_FOUND)
    }

    val branch = createBranch(projectId, name).also {
      it.originBranch = originBranch
      it.pending = true
    }
    branchRepository.save(branch)

    branchCopyService.copy(projectId, originBranch, branch)

    branch.pending = false
    branchRepository.save(branch)

    return branch
  }

  fun createBranch(projectId: Long, name: String): Branch {
    branchRepository.findByProjectIdAndName(projectId, name)?.let {
      throw BadRequestException(Message.BRANCH_ALREADY_EXISTS)
    }
    val project = entityManager.getReference(Project::class.java, projectId)

    return Branch().apply {
      this.project = project
      this.name = name
    }
  }

  @Transactional
  override fun deleteBranch(projectId: Long, branchId: Long) {
    val branch = getBranch(projectId, branchId)
    if (branch.isDefault) throw PermissionException(Message.CANNOT_DELETE_DEFAULT_BRANCH)
    branch.archivedAt = currentDateProvider.date
    applicationContext.publishEvent(OnBranchDeleted(branch))
  }

  @Transactional
  override fun dryRunMergeBranch(projectId: Long, request: DryRunMergeBranchRequest): BranchMerge {
    val sourceBranch = getBranch(projectId, request.sourceBranchId)
    val targetBranch = getBranch(projectId, request.targetBranchId)
    return branchMergeService.dryRun(sourceBranch, targetBranch)
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

  @Transactional
  override fun applyMerge(projectId: Long, mergeId: Long) {
    val merge = branchMergeService.getMerge(projectId, mergeId)
    if (!merge.isReadyToMerge) {
      if (!merge.isRevisionValid) {
        throw BadRequestException(Message.BRANCH_MERGE_REVISION_NOT_VALID)
      }
      if (!merge.isResolved) {
        throw BadRequestException(Message.BRANCH_MERGE_CONFLICTS_NOT_RESOLVED)
      }
    }
    merge.changes.forEach { change ->
      if (change.resolution == BranchKeyMergeResolutionType.SOURCE) {
        change.targetKey.merge(change.sourceKey)
      }
    }
  }
}
