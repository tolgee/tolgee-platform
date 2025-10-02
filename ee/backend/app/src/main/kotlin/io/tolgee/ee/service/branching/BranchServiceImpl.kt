package io.tolgee.ee.service.branching

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.ee.repository.BranchRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.service.branching.BranchCopyService
import io.tolgee.service.branching.BranchService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Primary
@Service
class BranchServiceImpl(
  private val branchRepository: BranchRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
  private val branchCopyService: BranchCopyService,
) : BranchService {
  override fun getAllBranches(projectId: Long, page: Pageable, search: String?): Page<Branch> {
    return branchRepository.getAllProjectBranches(projectId, page, search)
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
  }
}
