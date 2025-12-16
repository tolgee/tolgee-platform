package io.tolgee.ee.service.branching

import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class DefaultBranchCreator(
  private val keyService: KeyService,
  private val projectService: ProjectService,
) {
  @Transactional
  fun create(projectId: Long): Branch {
    val project = projectService.get(projectId)
    return create(project)
  }

  @Transactional
  fun create(project: Project): Branch {
    val branch = Branch.createMainBranch(project)
    keyService.getAll(project.id).forEach { it ->
      it.branch = branch
    }
    return branch
  }
}
