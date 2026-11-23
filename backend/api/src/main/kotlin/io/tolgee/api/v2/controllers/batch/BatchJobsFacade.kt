package io.tolgee.api.v2.controllers.batch

import io.tolgee.api.mcp.McpFacade
import io.tolgee.api.mcp.checkValid
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.BatchJobView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.SecurityService
import jakarta.validation.Validator
import org.springframework.stereotype.Service

@Service
class BatchJobsFacade(
  private val batchJobService: BatchJobService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val validator: Validator,
) : McpFacade {
  fun getView(jobId: Long): BatchJobView {
    val view = batchJobService.getView(projectHolder.project.id, jobId)
    if (!authenticationFacade.isAuthorSelfAccess(view.batchJob.author?.id)) {
      securityService.checkProjectPermission(projectHolder.project.id, Scope.BATCH_JOBS_VIEW)
    }
    return view
  }

  fun startMachineTranslation(data: MachineTranslationRequest): BatchJob {
    validator.checkValid(data)
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    return batchJobService.startJob(
      data,
      projectHolder.projectEntity,
      authenticationFacade.authenticatedUserEntity,
      BatchJobType.MACHINE_TRANSLATE,
    )
  }
}
