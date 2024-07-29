package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.task.*
import io.tolgee.hateoas.task.TaskModel
import io.tolgee.hateoas.task.TaskModelAssembler
import io.tolgee.hateoas.userAccount.UserAccountInProjectModel
import io.tolgee.hateoas.userAccount.UserAccountInProjectModelAssembler
import io.tolgee.model.views.ExtendedUserAccountInProject
import io.tolgee.model.views.KeysScopeView
import io.tolgee.model.views.TaskWithScopeView
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.TaskService
import io.tolgee.service.security.UserAccountService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/tasks",
    "/v2/projects/tasks",
  ],
)
@Tag(name = "Tasks", description = "Manipulates tasks")
@OpenApiOrderExtension(7)
class TaskController(
  private val taskService: TaskService,
  private val taskModelAssembler: TaskModelAssembler,
  private val pagedTaskResourcesAssembler: PagedResourcesAssembler<TaskWithScopeView>,
  private val projectHolder: ProjectHolder,
  private val userAccountService: UserAccountService,
  private val userAccountInProjectModelAssembler: UserAccountInProjectModelAssembler,
  private val pagedUserResourcesAssembler: PagedResourcesAssembler<ExtendedUserAccountInProject>,
) {
  @GetMapping("")
  @Operation(summary = "Get tasks")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getTasks(
    @ParameterObject
    filters: TaskFilters,
    @ParameterObject
    pageable: Pageable,
    @RequestParam("search", required = false)
    search: String?,
  ): PagedModel<TaskModel> {
    val tasks = taskService.getAllPaged(projectHolder.projectEntity, pageable, search, filters)
    return pagedTaskResourcesAssembler.toModel(tasks, taskModelAssembler)
  }

  @PostMapping("")
  @Operation(summary = "Create task")
  @UseDefaultPermissions
  @AllowApiAccess
  fun createTask(
    @RequestBody @Valid
    dto: CreateTaskRequest,
  ): TaskModel {
    val task = taskService.createTask(projectHolder.projectEntity, dto)

    return taskModelAssembler.toModel(task)
  }

  @GetMapping("/{taskId}")
  @Operation(summary = "Update task")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getTask(
    @PathVariable
    taskId: Long,
  ): TaskModel {
    val task = taskService.getTask(projectHolder.projectEntity, taskId)
    return taskModelAssembler.toModel(task)
  }

  @PutMapping("/{taskId}")
  @Operation(summary = "Update task")
  @UseDefaultPermissions
  @AllowApiAccess
  fun updateTask(
    @PathVariable
    taskId: Long,
    @RequestBody @Valid
    dto: UpdateTaskRequest,
  ): TaskModel {
    val task = taskService.updateTask(projectHolder.projectEntity, taskId, dto)
    return taskModelAssembler.toModel(task)
  }

  @DeleteMapping("/{taskId}")
  @Operation(summary = "Delete task")
  @UseDefaultPermissions
  @AllowApiAccess
  fun deleteTask(
    @PathVariable
    taskId: Long,
  ) {
    taskService.deleteTask(projectHolder.projectEntity, taskId)
  }

  @PutMapping("/{taskId}/keys")
  @Operation(summary = "Add or remove task keys")
  @UseDefaultPermissions
  @AllowApiAccess
  fun updateKeys(
    @PathVariable
    taskId: Long,
    @RequestBody @Valid
    dto: UpdateTaskKeysRequest,
  ) {
    taskService.updateTaskKeys(projectHolder.projectEntity, taskId, dto)
  }

  @PutMapping("/{taskId}/keys/{keyId}")
  @Operation(summary = "Add or remove task keys")
  @UseDefaultPermissions
  @AllowApiAccess
  fun updateTaskKey(
    @PathVariable
    taskId: Long,
    @PathVariable
    keyId: Long,
    @RequestBody @Valid
    dto: UpdateTaskKeyRequest,
  ) {
    taskService.updateTaskKey(projectHolder.projectEntity, taskId, keyId, dto)
  }

  @PostMapping("/create-multiple")
  @Operation(summary = "Create multiple tasks")
  @UseDefaultPermissions
  @AllowApiAccess
  fun createTasks(
    @RequestBody @Valid
    dto: CreateMultipleTasksRequest,
  ) {
    taskService.createMultipleTasks(projectHolder.projectEntity, dto.tasks)
  }

  @PostMapping("/calculate-scope")
  @Operation(summary = "Calculate scope")
  @UseDefaultPermissions
  @AllowApiAccess
  fun calculateScope(
    @RequestBody @Valid
    dto: CalculateScopeRequest,
  ): KeysScopeView {
    return taskService.calculateScope(projectHolder.projectEntity, dto)
  }

  @GetMapping("/possible-assignees")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getPossibleAssignees(
    @ParameterObject
    filters: UserAccountFilters,
    @ParameterObject
    pageable: Pageable,
    @RequestParam("search", required = false)
    search: String?,
  ): PagedModel<UserAccountInProjectModel> {
    val users =
      userAccountService.getAllInProjectWithPermittedLanguages(
        projectHolder.projectEntity.id,
        pageable,
        search,
        null,
        filters,
      )
    return pagedUserResourcesAssembler.toModel(users, userAccountInProjectModelAssembler)
  }
}
