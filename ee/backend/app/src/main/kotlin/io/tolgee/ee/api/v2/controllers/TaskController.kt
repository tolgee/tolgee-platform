package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.task.*
import io.tolgee.dtos.request.userAccount.UserAccountPermissionsFilters
import io.tolgee.ee.api.v2.hateoas.assemblers.TaskModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.TaskPerUserReportModelAssembler
import io.tolgee.ee.data.task.*
import io.tolgee.ee.service.TaskService
import io.tolgee.hateoas.task.TaskModel
import io.tolgee.hateoas.task.TaskPerUserReportModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TaskState
import io.tolgee.model.views.KeysScopeView
import io.tolgee.model.views.TaskWithScopeView
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.security.SecurityService
import io.tolgee.service.security.UserAccountService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
  private val userAccountModelAssembler: SimpleUserAccountModelAssembler,
  private val pagedUserResourcesAssembler: PagedResourcesAssembler<UserAccount>,
  private val taskPerUserReportModelAssembler: TaskPerUserReportModelAssembler,
  private val securityService: SecurityService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @GetMapping("")
  @Operation(summary = "Get tasks")
  @RequiresProjectPermissions([Scope.TASKS_VIEW])
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
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_CREATE)
  fun createTask(
    @RequestBody @Valid
    dto: CreateTaskRequest,
    @ParameterObject
    filters: TranslationScopeFilters,
  ): TaskModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TASKS,
    )
    val task = taskService.createTask(projectHolder.projectEntity, dto, filters)
    return taskModelAssembler.toModel(task)
  }

  @PostMapping("/create-multiple")
  @Operation(summary = "Create multiple tasks")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASKS_CREATE)
  fun createTasks(
    @RequestBody @Valid
    dto: CreateMultipleTasksRequest,
    @ParameterObject
    filters: TranslationScopeFilters,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TASKS,
    )

    taskService.createMultipleTasks(projectHolder.projectEntity, dto.tasks, filters)
  }

  @GetMapping("/{taskNumber}")
  @Operation(summary = "Get task")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    // user can view tasks assigned to him
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.projectEntity.id, taskNumber)
    val task = taskService.getTask(projectHolder.projectEntity, taskNumber)
    return taskModelAssembler.toModel(task)
  }

  @PutMapping("/{taskNumber}")
  @Operation(summary = "Update task")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_UPDATE)
  fun updateTask(
    @PathVariable
    taskNumber: Long,
    @RequestBody @Valid
    dto: UpdateTaskRequest,
  ): TaskModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TASKS,
    )

    val task = taskService.updateTask(projectHolder.projectEntity, taskNumber, dto)
    return taskModelAssembler.toModel(task)
  }

  @GetMapping("/{taskNumber}/per-user-report")
  @Operation(summary = "Report who did what")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getPerUserReport(
    @PathVariable
    taskNumber: Long,
  ): List<TaskPerUserReportModel> {
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.projectEntity.id, taskNumber)

    val result = taskService.getReport(projectHolder.projectEntity, taskNumber)
    return result.map { taskPerUserReportModelAssembler.toModel(it) }
  }

  @GetMapping("/{taskNumber}/xlsx-report")
  @Operation(summary = "Report who did what")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getXlsxReport(
    @PathVariable
    taskNumber: Long,
  ): ResponseEntity<ByteArrayResource> {
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.projectEntity.id, taskNumber)
    val byteArray = taskService.getExcelFile(projectHolder.projectEntity, taskNumber)
    val resource = ByteArrayResource(byteArray)

    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_OCTET_STREAM
    headers.setContentDispositionFormData("attachment", "report.xlsx")
    headers.contentLength = byteArray.size.toLong()

    return ResponseEntity(resource, headers, HttpStatus.OK)
  }

  @GetMapping("/{taskNumber}/keys")
  @Operation(summary = "Get task keys")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getTaskKeys(
    @PathVariable
    taskNumber: Long,
  ): TaskKeysResponse {
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.projectEntity.id, taskNumber)
    return TaskKeysResponse(
      keys = taskService.getTaskKeys(projectHolder.projectEntity, taskNumber),
    )
  }

  @PutMapping("/{taskNumber}/keys")
  @Operation(summary = "Add or remove task keys")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_KEYS_UPDATE)
  fun updateTaskKeys(
    @PathVariable
    taskNumber: Long,
    @RequestBody @Valid
    dto: UpdateTaskKeysRequest,
  ) {
    taskService.updateTaskKeys(projectHolder.projectEntity, taskNumber, dto)
  }

  @GetMapping("/{taskNumber}/blocking-tasks")
  @Operation(summary = "Get task ids which block this task")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getBlockingTasks(
    @PathVariable
    taskNumber: Long,
  ): List<Long> {
    return taskService.getBlockingTasks(projectHolder.projectEntity, taskNumber)
  }

  @PostMapping("/{taskNumber}/finish")
  @Operation(summary = "Finish task")
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_FINISH)
  fun finishTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    // user can only finish tasks assigned to him
    securityService.hasTaskEditScopeOrIsAssigned(projectHolder.projectEntity.id, taskNumber)
    val task = taskService.setTaskState(projectHolder.projectEntity, taskNumber, TaskState.DONE)
    return taskModelAssembler.toModel(task)
  }

  @PostMapping("/{taskNumber}/close")
  @Operation(summary = "Close task")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_CLOSE)
  fun closeTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    val task = taskService.setTaskState(projectHolder.projectEntity, taskNumber, TaskState.CLOSED)
    return taskModelAssembler.toModel(task)
  }

  @PostMapping("/{taskNumber}/reopen")
  @Operation(summary = "Reopen task")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_REOPEN)
  fun reopenTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TASKS,
    )
    val task = taskService.setTaskState(projectHolder.projectEntity, taskNumber, TaskState.IN_PROGRESS)
    return taskModelAssembler.toModel(task)
  }

  @PutMapping("/{taskNumber}/keys/{keyId}")
  @Operation(
    summary = "Update task key",
    description = "Mark key as done, which updates task progress.",
  )
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  fun updateTaskKey(
    @PathVariable
    taskNumber: Long,
    @PathVariable
    keyId: Long,
    @RequestBody @Valid
    dto: UpdateTaskKeyRequest,
  ): UpdateTaskKeyResponse {
    // user can only update tasks assigned to him
    securityService.hasTaskEditScopeOrIsAssigned(projectHolder.projectEntity.id, taskNumber)
    return taskService.updateTaskKey(projectHolder.projectEntity, taskNumber, keyId, dto)
  }

  @PostMapping("/calculate-scope")
  @Operation(summary = "Calculate scope")
  @RequiresProjectPermissions([Scope.TASKS_VIEW])
  @AllowApiAccess
  fun calculateScope(
    @RequestBody @Valid
    dto: CalculateScopeRequest,
    @ParameterObject
    filters: TranslationScopeFilters,
  ): KeysScopeView {
    return taskService.calculateScope(projectHolder.projectEntity, dto, filters)
  }

  @GetMapping("/possible-assignees")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  fun getPossibleAssignees(
    @ParameterObject
    filters: UserAccountPermissionsFilters,
    @ParameterObject
    pageable: Pageable,
    @RequestParam("search", required = false)
    search: String?,
  ): PagedModel<SimpleUserAccountModel> {
    val users =
      userAccountService.findWithMinimalPermissions(
        filters,
        projectHolder.projectEntity.id,
        search,
        pageable,
      )
    return pagedUserResourcesAssembler.toModel(users, userAccountModelAssembler)
  }
}
