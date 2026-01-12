package io.tolgee.core.domain.project.service

import io.tolgee.core.concepts.types.FailureMarker
import io.tolgee.core.concepts.types.OutputMarker
import io.tolgee.core.domain.project.data.ProjectId
import io.tolgee.model.Project
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

interface IFetchProjects {
  sealed interface Output : OutputMarker {
    data class Success(val project: Project) : Output

    data object NotFound : Output, FailureMarker
  }

  fun byId(projectId: ProjectId): Output
}

@Service
@Transactional(propagation = Propagation.MANDATORY)
class IFetchProjectsImpl(
  private val queryProjects: IQueryProjects,
) : IFetchProjects {
  override fun byId(projectId: ProjectId): IFetchProjects.Output {
    val project = queryProjects.find(projectId.value)
      ?: return IFetchProjects.Output.NotFound

    return IFetchProjects.Output.Success(project)
  }
}
