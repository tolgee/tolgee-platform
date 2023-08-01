package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobCancellationManager
import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.BatchJobService
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.BatchJobView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.security.SecurityService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/", "/v2/projects/"])
@Tag(name = "Batch Operations management")
@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
class BatchJobManagementController(
  private val batchJobCancellationManager: BatchJobCancellationManager,
  private val batchJobService: BatchJobService,
  private val projectHolder: ProjectHolder,
  private val batchJobModelAssembler: BatchJobModelAssembler,
  private val pagedResourcesAssembler: PagedResourcesAssembler<BatchJobView>,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService
) {
  @GetMapping(value = ["batch-jobs"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.BATCH_JOBS_VIEW)
  @Operation(summary = "Lists all batch operations in project")
  fun list(@Valid @ParameterObject @SortDefault("id") pageable: Pageable): PagedModel<BatchJobModel> {
    val views = batchJobService.getViews(projectHolder.project.id, null, pageable)
    return pagedResourcesAssembler.toModel(views, batchJobModelAssembler)
  }

  @GetMapping(value = ["my-batch-jobs"])
  @AccessWithApiKey()
  @AccessWithAnyProjectPermission()
  @Operation(summary = "Lists all batch operations in project started by current user")
  fun myList(@Valid @ParameterObject @SortDefault("id") pageable: Pageable): PagedModel<BatchJobModel> {
    val views = batchJobService.getViews(
      projectId = projectHolder.project.id,
      userAccount = authenticationFacade.userAccount,
      pageable = pageable
    )
    return pagedResourcesAssembler.toModel(views, batchJobModelAssembler)
  }

  @GetMapping(value = ["current-batch-jobs"])
  @AccessWithApiKey()
  @AccessWithAnyProjectPermission()
  @Operation(
    summary = "Returns all running and pending batch operations",
    description = "Completed batch operations are returned only if they are not older than 1 hour. " +
      "If user doesn't have permission to view all batch operations, only their operations are returned."
  )
  fun currentJobs(): CollectionModel<BatchJobModel> {
    val views = batchJobService.getCurrentJobViews(
      projectId = projectHolder.project.id,
    )
    return batchJobModelAssembler.toCollectionModel(views)
  }

  @GetMapping(value = ["batch-jobs/{id}"])
  @AccessWithApiKey()
  @AccessWithAnyProjectPermission()
  @Operation(summary = "Returns batch operation")
  fun get(@PathVariable id: Long): BatchJobModel {
    val view = batchJobService.getView(id)
    checkViewPermission(view.batchJob)
    return batchJobModelAssembler.toModel(view)
  }

  @PutMapping(value = ["batch-jobs/{id}/cancel"])
  @AccessWithApiKey()
  @AccessWithAnyProjectPermission()
  @Operation(summary = "Stops batch operation (if possible)")
  fun cancel(@PathVariable id: Long) {
    checkCancelPermission(batchJobService.getJobDto(id))
    batchJobCancellationManager.cancel(id)
  }

  private fun checkViewPermission(job: BatchJob) {
    if (job.author?.id == authenticationFacade.userAccount.id) {
      return
    }
    securityService.checkProjectPermission(projectHolder.project.id, Scope.BATCH_JOBS_VIEW)
  }

  private fun checkCancelPermission(job: BatchJobDto) {
    if (job.authorId == authenticationFacade.userAccount.id) {
      return
    }
    securityService.checkProjectPermission(projectHolder.project.id, Scope.BATCH_JOBS_CANCEL)
  }
}
