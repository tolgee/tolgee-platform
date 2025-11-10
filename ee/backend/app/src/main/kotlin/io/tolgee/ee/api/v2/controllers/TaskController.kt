package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.userAccount.UserAccountPermissionsFilters
import io.tolgee.ee.api.v2.hateoas.assemblers.TaskModelAssembler
import io.tolgee.ee.api.v2.hateoas.assemblers.TaskPerUserReportModelAssembler
import io.tolgee.ee.data.task.CalculateScopeRequest
import io.tolgee.ee.data.task.CreateMultipleTasksRequest
import io.tolgee.ee.data.task.CreateTaskRequest
import io.tolgee.ee.data.task.TaskFilters
import io.tolgee.ee.data.task.TaskKeysResponse
import io.tolgee.ee.data.task.TranslationScopeFilters
import io.tolgee.ee.data.task.UpdateTaskKeyRequest
import io.tolgee.ee.data.task.UpdateTaskKeyResponse
import io.tolgee.ee.data.task.UpdateTaskKeysRequest
import io.tolgee.ee.data.task.UpdateTaskRequest
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
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOneOfFeatures
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
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/tasks",
    "/v2/projects/tasks",
  ],
)
@Tag(name = "Tasks", description = "Manipulates tasks")
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
) {
  @PostMapping("")
  @Operation(summary = "Create task")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_CREATE)
  @OpenApiOrderExtension(1)
  @RequiresFeatures(Feature.TASKS)
  fun createTask(
    @RequestBody @Valid
    dto: CreateTaskRequest,
    @ParameterObject
    filters: TranslationScopeFilters,
  ): TaskModel {
    val task = taskService.createTask(projectHolder.project.id, dto, filters)
    return taskModelAssembler.toModel(task)
  }

  @PostMapping("/create-multiple-tasks")
  @Operation(summary = "Create multiple tasks")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASKS_CREATE)
  @OpenApiOrderExtension(2)
  @RequiresFeatures(Feature.TASKS)
  fun createTasks(
    @RequestBody @Valid
    dto: CreateMultipleTasksRequest,
    @ParameterObject
    filters: TranslationScopeFilters,
  ) {
    taskService.createMultipleTasks(projectHolder.project.id, dto.tasks, filters)
  }

  @GetMapping("/{taskNumber}")
  @Operation(summary = "Get task")
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  @OpenApiOrderExtension(3)
  fun getTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    // users can view tasks assigned to them
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.project.id, taskNumber)
    val task = taskService.getTask(projectHolder.project.id, taskNumber)
    return taskModelAssembler.toModel(task)
  }

  @GetMapping("")
  @Operation(summary = "Get tasks")
  @RequiresProjectPermissions([Scope.TASKS_VIEW])
  @OpenApiOrderExtension(4)
  @AllowApiAccess
  fun getTasks(
    @ParameterObject
    filters: TaskFilters,
    @ParameterObject
    pageable: Pageable,
    @RequestParam("search", required = false)
    search: String?,
  ): PagedModel<TaskModel> {
    val tasks = taskService.getAllPaged(projectHolder.project.id, pageable, search, filters)
    return pagedTaskResourcesAssembler.toModel(tasks, taskModelAssembler)
  }

  @PutMapping("/{taskNumber}")
  @Operation(summary = "Update task")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_UPDATE)
  @OpenApiOrderExtension(5)
  @RequiresOneOfFeatures(Feature.TASKS, Feature.ORDER_TRANSLATION)
  fun updateTask(
    @PathVariable
    taskNumber: Long,
    @RequestBody @Valid
    dto: UpdateTaskRequest,
  ): TaskModel {
    val task = taskService.updateTask(projectHolder.project.id, taskNumber, dto)
    return taskModelAssembler.toModel(task)
  }

  @PutMapping("/{taskNumber}/finish")
  @Operation(summary = "Finish task")
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_FINISH)
  @OpenApiOrderExtension(6)
  fun finishTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    // users can only finish tasks assigned to them
    securityService.hasTaskEditScopeOrIsAssigned(projectHolder.project.id, taskNumber)
    val task = taskService.setTaskState(projectHolder.project.id, taskNumber, TaskState.FINISHED)
    return taskModelAssembler.toModel(task)
  }

  @PutMapping("/{taskNumber}/close")
  @Operation(summary = "Close task", deprecated = true)
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_CLOSE)
  @OpenApiOrderExtension(7)
  fun closeTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    val task = taskService.setTaskState(projectHolder.project.id, taskNumber, TaskState.CANCELED)
    return taskModelAssembler.toModel(task)
  }

  @PutMapping("/{taskNumber}/cancel")
  @Operation(summary = "Close task")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_CLOSE)
  @OpenApiOrderExtension(7)
  fun cancelTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    val task = taskService.setTaskState(projectHolder.project.id, taskNumber, TaskState.CANCELED)
    return taskModelAssembler.toModel(task)
  }

  @PutMapping("/{taskNumber}/reopen")
  @Operation(summary = "Reopen task")
  @RequiresProjectPermissions([Scope.TASKS_EDIT])
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_REOPEN)
  @OpenApiOrderExtension(8)
  @RequiresOneOfFeatures(Feature.TASKS, Feature.ORDER_TRANSLATION)
  fun reopenTask(
    @PathVariable
    taskNumber: Long,
  ): TaskModel {
    val task = taskService.setTaskState(projectHolder.project.id, taskNumber, TaskState.IN_PROGRESS)
    return taskModelAssembler.toModel(task)
  }

  @GetMapping("/{taskNumber}/per-user-report")
  @Operation(
    summary = "Get report",
    description = "Detailed statistics for every assignee",
  )
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  fun getPerUserReport(
    @PathVariable
    taskNumber: Long,
  ): List<TaskPerUserReportModel> {
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.project.id, taskNumber)

    val result = taskService.getReport(projectHolder.projectEntity, taskNumber)
    return result.map { taskPerUserReportModelAssembler.toModel(it) }
  }

  @GetMapping("/{taskNumber}/xlsx-report")
  @Operation(
    summary = "Get report in XLSX",
    description = "Detailed statistics about the task results",
  )
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  fun getXlsxReport(
    @PathVariable
    taskNumber: Long,
  ): ResponseEntity<ByteArrayResource> {
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.project.id, taskNumber)
    val byteArray = taskService.getExcelFile(projectHolder.projectEntity, taskNumber)
    val resource = ByteArrayResource(byteArray)

    val headers = HttpHeaders()
    headers.contentType = MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    headers.setContentDispositionFormData("attachment", "report.xlsx")
    headers.contentLength = byteArray.size.toLong()

    return ResponseEntity(resource, headers, HttpStatus.OK)
  }

  @GetMapping("/{taskNumber}/keys")
  @Operation(summary = "Get task keys")
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  fun getTaskKeys(
    @PathVariable
    taskNumber: Long,
  ): TaskKeysResponse {
    securityService.hasTaskViewScopeOrIsAssigned(projectHolder.project.id, taskNumber)
    return TaskKeysResponse(
      keys = taskService.getTaskKeys(projectHolder.project.id, taskNumber),
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
    taskService.updateTaskKeys(projectHolder.project.id, taskNumber, dto)
  }

  @GetMapping("/{taskNumber}/blocking-tasks")
  @Operation(
    summary = "Get blocking task numbers",
    description = "If the tasks is blocked by other tasks, it returns numbers of these tasks.",
  )
  @UseDefaultPermissions
  @AllowApiAccess
  fun getBlockingTasks(
    @PathVariable
    taskNumber: Long,
  ): List<Long> {
    return taskService.getBlockingTasks(projectHolder.project.id, taskNumber)
  }

  @PutMapping("/{taskNumber}/keys/{keyId}")
  @Operation(
    summary = "Update task key",
    description = "Mark key as done, which updates task progress.",
  )
  // permissions checked inside
  @UseDefaultPermissions
  @AllowApiAccess
  @RequestActivity(ActivityType.TASK_KEY_UPDATE)
  fun updateTaskKey(
    @PathVariable
    taskNumber: Long,
    @PathVariable
    keyId: Long,
    @RequestBody @Valid
    dto: UpdateTaskKeyRequest,
  ): UpdateTaskKeyResponse {
    // users can only update tasks assigned to them
    securityService.hasTaskEditScopeOrIsAssigned(projectHolder.project.id, taskNumber)
    return taskService.updateTaskKey(projectHolder.project.id, taskNumber, keyId, dto)
  }

  @PostMapping("/calculate-scope")
  @Operation(summary = "Calculate scope")
  @ReadOnlyOperation
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
  @Operation(summary = "Get possible assignees")
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
        projectHolder.project.id,
        search,
        pageable,
      )
    return pagedUserResourcesAssembler.toModel(users, userAccountModelAssembler)
  }
}
