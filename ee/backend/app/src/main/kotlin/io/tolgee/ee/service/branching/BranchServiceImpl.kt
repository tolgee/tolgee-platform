package io.tolgee.ee.service.branching

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.ee.api.v2.hateoas.model.branching.CreateBranchModel
import io.tolgee.ee.repository.BranchRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.service.branching.BranchService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Primary
@Service
class BranchServiceImpl(
  private val branchRepository: BranchRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
) : BranchService {
  override fun getAllBranches(projectId: Long, page: Pageable, search: String?): Page<Branch> {
    return branchRepository.getAllProjectBranches(projectId, page, search)
  }

  @Transactional
  fun createBranch(projectId: Long, branch: CreateBranchModel): Branch {
    branchRepository.findByProjectIdAndName(projectId, branch.name)?.let {
      throw BadRequestException(Message.BRANCH_ALREADY_EXISTS)
    }
    val project = entityManager.getReference(Project::class.java, projectId)
    val originBranch = branchRepository.findByIdOrNull(branch.originBranchId)

    val branch = Branch().apply {
      this.project = project
      this.name = branch.name
      this.originBranch = originBranch
    }
    branchRepository.save(branch)
    return branch
  }

  @Transactional
  fun deleteBranch(projectId: Long, branchId: Long) {
    val branch =
      branchRepository.findByProjectIdAndId(projectId, branchId) ?: throw BadRequestException(Message.BRANCH_NOT_FOUND)
    if (branch.isDefault) throw PermissionException(Message.CANNOT_DELETE_DEFAULT_BRANCH)
    branch.archivedAt = currentDateProvider.date
  }
}
