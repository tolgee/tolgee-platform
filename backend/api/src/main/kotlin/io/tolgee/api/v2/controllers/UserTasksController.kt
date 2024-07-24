package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.task.TaskWithProjectModel
import io.tolgee.hateoas.task.TaskWithProjectModelAssembler
import io.tolgee.model.task.Task
import io.tolgee.model.views.TaskWithScopeView
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.TaskService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/user-tasks"])
@Tag(name = "User tasks")
class UserTasksController(
  val taskService: TaskService,
  val authenticationFacade: AuthenticationFacade,
  private val pagedTaskResourcesAssembler: PagedResourcesAssembler<TaskWithScopeView>,
  private val taskWithProjectModelAssembler: TaskWithProjectModelAssembler,
) {
  @GetMapping("")
  @Operation(summary = "Get user tasks")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getTasks(
    @ParameterObject
    pageable: Pageable,
  ): PagedModel<TaskWithProjectModel> {
    val user = authenticationFacade.authenticatedUser
    val tasks = taskService.getUserTasksPaged(user.id, pageable)
    return pagedTaskResourcesAssembler.toModel(tasks, taskWithProjectModelAssembler)
  }
}
