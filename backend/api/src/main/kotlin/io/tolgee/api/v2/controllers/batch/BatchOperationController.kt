package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobActionService
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.request.BatchTranslateRequest
import io.tolgee.model.enums.Scope
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.security.SecurityService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/", "/v2/projects/"])
@Tag(name = "Batch operations")
@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
class BatchOperationController(
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val batchJobService: BatchJobService,
  private val authenticationFacade: AuthenticationFacade,
  private val batchJobActionService: BatchJobActionService
) {
  @GetMapping(value = ["batch-operations"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.BATCH_OPERATIONS_VIEW)
  @Operation(summary = "Lists all batch operations in project")
  fun list(@Valid @RequestBody data: BatchTranslateRequest) {
  }

  @GetMapping(value = ["my-batch-operations"])
  @AccessWithApiKey()
  @AccessWithAnyProjectPermission()
  @Operation(summary = "Lists all batch operations in project started by current user")
  fun myList(@Valid @ParameterObject pageable: Pageable) {
  }

  @GetMapping(value = ["batch-operations/{id}"])
  @AccessWithApiKey()
  @AccessWithAnyProjectPermission()
  @Operation(summary = "Lists all batch operations in project started by current user")
  fun get(@Valid @ParameterObject pageable: Pageable) {
  }

  @PutMapping(value = ["batch-operations/{id}/cancel"])
  @AccessWithApiKey()
  @AccessWithAnyProjectPermission()
  @Operation(summary = "Stops batch operation (if possible)")
  fun cancel(@PathVariable id: Long) {
    batchJobActionService.cancel(id)
  }
}
