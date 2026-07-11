package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.assemblers.TaskWithProjectModelAssembler
import io.tolgee.ee.data.task.TaskFilters
import io.tolgee.ee.service.TaskService
import io.tolgee.hateoas.task.TaskWithProjectModel
import io.tolgee.model.views.TaskWithScopeView
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.UseDefaultPermissions
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/user-tasks"])
@Tag(name = "User tasks")
class UserTasksController(
  private val taskService: TaskService,
  private val authenticationFacade: AuthenticationFacade,
  private val pagedTaskResourcesAssembler: PagedResourcesAssembler<TaskWithScopeView>,
  private val taskWithProjectModelAssembler: TaskWithProjectModelAssembler,
) {
  @GetMapping("")
  @Operation(summary = "Get user tasks")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getTasks(
    @ParameterObject
    filters: TaskFilters,
    @ParameterObject
    pageable: Pageable,
    @RequestParam("search", required = false)
    search: String?,
  ): PagedModel<TaskWithProjectModel> {
    val user = authenticationFacade.authenticatedUser
    val tasks = taskService.getUserTasksPaged(user.id, pageable, search, filters)
    return pagedTaskResourcesAssembler.toModel(tasks, taskWithProjectModelAssembler)
  }
}
