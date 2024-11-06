package io.tolgee.ee.service

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.ee.component.TaskReportHelper
import io.tolgee.ee.data.task.CalculateScopeRequest
import io.tolgee.ee.data.task.CreateTaskRequest
import io.tolgee.ee.data.task.TaskFilters
import io.tolgee.ee.data.task.TranslationScopeFilters
import io.tolgee.ee.data.task.UpdateTaskKeyRequest
import io.tolgee.ee.data.task.UpdateTaskKeyResponse
import io.tolgee.ee.data.task.UpdateTaskKeysRequest
import io.tolgee.ee.data.task.UpdateTaskRequest
import io.tolgee.ee.repository.TaskRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.views.KeysScopeView
import io.tolgee.model.views.TaskPerUserReportView
import io.tolgee.model.views.TaskWithScopeView
import io.tolgee.model.views.TranslationToTaskView
import io.tolgee.repository.TaskKeyRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ITaskService
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.executeInNewRepeatableTransaction
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.apache.commons.io.output.ByteArrayOutputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.util.*
import kotlin.math.max

@Component
class TaskService(
  private val taskRepository: TaskRepository,
  private val entityManager: EntityManager,
  private val languageService: LanguageService,
  @Lazy
  private val securityService: SecurityService,
  private val taskKeyRepository: TaskKeyRepository,
  private val authenticationFacade: AuthenticationFacade,
  @Lazy
  @Autowired
  private val taskService: TaskService,
  private val assigneeNotificationService: AssigneeNotificationService,
  @Autowired
  private val platformTransactionManager: PlatformTransactionManager,
  private val currentDateProvider: CurrentDateProvider,
  private val keyService: KeyService,
  private val projectService: ProjectService,
) : ITaskService {
  fun getAllPaged(
    projectId: Long,
    pageable: Pageable,
    search: String?,
    filters: TaskFilters,
  ): Page<TaskWithScopeView> {
    val pagedTasks = taskRepository.getAllByProjectId(projectId, pageable, search, filters)
    val withPrefetched = getPrefetchedTasks(pagedTasks.content)
    return PageImpl(getTasksWithScope(withPrefetched), pageable, pagedTasks.totalElements)
  }

  fun getUserTasksPaged(
    userId: Long,
    pageable: Pageable,
    search: String?,
    filters: TaskFilters,
  ): Page<TaskWithScopeView> {
    val pagedTasks = taskRepository.getAllByAssignee(userId, pageable, search, filters)
    val withPrefetched = getPrefetchedTasks(pagedTasks.content)
    return PageImpl(getTasksWithScope(withPrefetched), pageable, pagedTasks.totalElements)
  }

  fun getPrefetchedTasks(tasks: Collection<Task>): List<Task> {
    val ids = tasks.map { it.id }.mapIndexed { i, v -> Pair(v, i) }.toMap()
    val data = taskRepository.getByIdsWithAllPrefetched(tasks)
    // return tasks in the same order
    return data.sortedBy { ids[it.id] }
  }

  @Transactional
  fun createMultipleTasks(
    projectId: Long,
    dtos: Collection<CreateTaskRequest>,
    filters: TranslationScopeFilters,
  ) {
    dtos.forEach {
      createSingleTask(projectId, it, filters)
    }
  }

  @Transactional
  fun createTask(
    projectId: Long,
    dto: CreateTaskRequest,
    filters: TranslationScopeFilters,
  ): TaskWithScopeView {
    val task = taskService.createSingleTask(projectId, dto, filters)
    val prefetched = taskService.getPrefetchedTasks(listOf(task)).first()
    return getTaskWithScope(prefetched)
  }

  fun getNextTaskNumber(projectId: Long): Long {
    val lastTaskNumber =
      taskRepository.findByProjectOrderByNumberDesc(
        entityManager.getReference(Project::class.java, projectId),
      ).firstOrNull()?.number ?: 0L
    val projectLastTaskNumber = projectService.get(projectId).lastTaskNumber
    // use whichever is larger
    return max(lastTaskNumber, projectLastTaskNumber) + 1
  }

  @Transactional
  fun createSingleTask(
    projectId: Long,
    dto: CreateTaskRequest,
    filters: TranslationScopeFilters,
  ): Task {
    var lastErr = DataIntegrityViolationException("Error")
    repeat(10) {
      // necessary for proper transaction creation
      try {
        return executeInNewRepeatableTransaction(platformTransactionManager) {
          val task = taskService.createTaskInTransaction(projectId, dto, filters)
          entityManager.flush()
          task.assignees.forEach {
            assigneeNotificationService.notifyNewAssignee(it, task)
          }
          task
        }
      } catch (e: DataIntegrityViolationException) {
        lastErr = e
      }
    }
    throw lastErr
  }

  @Transactional()
  fun createTaskInTransaction(
    projectId: Long,
    dto: CreateTaskRequest,
    filters: TranslationScopeFilters,
  ): Task {
    val newNumber = getNextTaskNumber(projectId)
    val language = checkLanguage(dto.languageId!!, projectId)
    val assignees = checkAssignees(dto.assignees ?: mutableSetOf(), projectId)
    val keyIds =
      getOnlyProjectKeys(
        projectId,
        dto.languageId!!,
        dto.type,
        dto.keys ?: mutableSetOf(),
        filters,
      )

    val task = Task()

    task.number = newNumber
    task.project = entityManager.getReference(Project::class.java, projectId)
    task.name = dto.name
    task.type = dto.type
    task.description = dto.description
    task.dueDate = dto.dueDate?.let { Date(it) }
    task.language = language
    task.assignees = assignees
    task.author = entityManager.getReference(UserAccount::class.java, authenticationFacade.authenticatedUser.id)
    task.state = TaskState.NEW
    taskRepository.saveAndFlush(task)
    val keys = keyService.getByIds(keyIds)
    val taskKeys = keys.map { TaskKey(task, it) }.toMutableSet()
    task.keys = taskKeys
    taskKeyRepository.saveAll(taskKeys)

    projectService.updateLastTaskNumber(projectId, newNumber)
    return task
  }

  @Transactional
  fun updateTask(
    projectId: Long,
    taskNumber: Long,
    dto: UpdateTaskRequest,
  ): TaskWithScopeView {
    val task = findByNumber(projectId, taskNumber)

    task.name = dto.name
    task.description = dto.description

    task.dueDate = dto.dueDate?.let { Date(it) }

    val newAssignees = checkAssignees(dto.assignees, projectId)
    newAssignees.forEach {
      if (!task.assignees.contains(it)) {
        assigneeNotificationService.notifyNewAssignee(it, task)
      }
    }
    task.assignees = newAssignees

    taskRepository.saveAndFlush(task)
    return getTaskWithScope(task)
  }

  @Transactional
  fun setTaskState(
    projectId: Long,
    taskNumber: Long,
    state: TaskState,
  ): TaskWithScopeView {
    val task = findByNumber(projectId, taskNumber)
    val taskWithScope = getTaskWithScope(task)
    if (state == TaskState.DONE && taskWithScope.doneItems != taskWithScope.totalItems) {
      throw BadRequestException(Message.TASK_NOT_FINISHED)
    }
    if (state == TaskState.NEW || state == TaskState.IN_PROGRESS) {
      task.state = if (taskWithScope.doneItems == 0L) TaskState.NEW else TaskState.IN_PROGRESS
    } else {
      task.closedAt = currentDateProvider.date
      task.state = state
    }
    taskRepository.saveAndFlush(task)
    return getTask(projectId, taskNumber)
  }

  @Transactional
  fun getTask(
    projectId: Long,
    taskNumber: Long,
  ): TaskWithScopeView {
    val task = findByNumber(projectId, taskNumber)
    return getTaskWithScope(task)
  }

  @Transactional
  fun updateTaskKeys(
    projectId: Long,
    taskNumber: Long,
    dto: UpdateTaskKeysRequest,
  ) {
    val task = findByNumber(projectId, taskNumber)

    dto.removeKeys?.let { toRemove ->
      val taskKeysToRemove =
        task.keys.filter {
          toRemove.contains(
            it.key.id,
          )
        }.toMutableSet()
      task.keys = task.keys.subtract(taskKeysToRemove).toMutableSet()
      taskKeyRepository.deleteAll(taskKeysToRemove)
    }

    dto.addKeys?.let { toAdd ->
      val existingKeys = task.keys.map { it.key.id }.toMutableSet()
      val nonExistingKeyIds = toAdd.subtract(existingKeys).toMutableSet()
      val keysToAdd = keyService.getByIds(nonExistingKeyIds)
      val taskKeysToAdd =
        keysToAdd
          .filter { key -> nonExistingKeyIds.contains(key.id) }
          .map { key -> TaskKey(task, key) }
      task.keys = task.keys.union(taskKeysToAdd).toMutableSet()
      taskKeyRepository.saveAll(taskKeysToAdd)
    }
  }

  @Transactional
  fun updateTaskKey(
    projectId: Long,
    taskNumber: Long,
    keyId: Long,
    dto: UpdateTaskKeyRequest,
  ): UpdateTaskKeyResponse {
    val task = findByNumber(projectId, taskNumber)

    if (task.state == TaskState.CLOSED || task.state == TaskState.DONE) {
      throw BadRequestException(Message.TASK_NOT_OPEN)
    }

    val taskWithScope = getTaskWithScope(task)

    val taskKey = findTaskKey(task.id, keyId)

    val previousValue = taskKey.done
    val changed = previousValue != dto.done

    val totalDone = taskWithScope.doneItems + if (dto.done) 1 else -1
    val allDone = totalDone == taskWithScope.totalItems
    if (changed) {
      taskKey.author = getTaskKeyAuthorByState(dto.done)
      taskKey.done = dto.done
      taskKeyRepository.save(taskKey)
      task.state = if (totalDone == 0L) TaskState.NEW else TaskState.IN_PROGRESS
      taskRepository.save(task)
    }

    return UpdateTaskKeyResponse(
      done = taskKey.done,
      taskFinished = allDone,
    )
  }

  override fun deleteAll(tasks: List<Task>) {
    for (task in tasks) {
      taskKeyRepository.deleteAll(task.keys)
      taskRepository.delete(task)
    }
  }

  override fun findAssigneeById(
    projectId: Long,
    taskNumber: Long,
    userId: Long,
  ): List<UserAccount> {
    return taskRepository.findAssigneeById(projectId, taskNumber, userId)
  }

  override fun findAssigneeByKey(
    keyId: Long,
    languageId: Long,
    userId: Long,
    type: TaskType?,
  ): List<UserAccount> {
    return taskRepository.findAssigneeByKey(keyId, languageId, userId, type)
  }

  @Transactional
  fun calculateScope(
    projectEntity: Project,
    dto: CalculateScopeRequest,
    filters: TranslationScopeFilters,
  ): KeysScopeView {
    val language = languageService.get(dto.languageId, projectEntity.id)
    val keysIncludingConflicts =
      taskRepository.getKeysIncludingConflicts(
        projectEntity.id,
        language.id,
        dto.keys,
        filters,
      )
    val relevantKeys =
      taskRepository.getKeysWithoutConflicts(
        projectEntity.id,
        language.id,
        dto.type.toString(),
        dto.keys,
        filters,
      )
    val result =
      taskRepository.calculateScope(
        projectEntity.id,
        projectEntity.baseLanguage!!.id,
        relevantKeys,
      )
    return KeysScopeView(
      keyCount = relevantKeys.size.toLong(),
      wordCount = result.wordCount,
      characterCount = result.characterCount,
      keyCountIncludingConflicts = keysIncludingConflicts.size.toLong(),
    )
  }

  @Transactional
  fun getTaskKeys(
    projectId: Long,
    taskNumber: Long,
  ): List<Long> {
    return taskRepository.getTaskKeys(projectId, taskNumber)
  }

  fun getBlockingTasks(
    projectId: Long,
    taskNumber: Long,
  ): List<Long> {
    return taskRepository.getBlockingTaskNumbers(projectId, taskNumber)
  }

  override fun getKeysWithTasks(
    userId: Long,
    keyIds: Collection<Long>,
  ): Map<Long, List<TranslationToTaskView>> {
    val data = taskRepository.getByKeyId(userId, keyIds)
    val result = mutableMapOf<Long, MutableList<TranslationToTaskView>>()
    data.forEach {
      val existing = result[it.keyId] ?: mutableListOf()
      existing.add(it)
      result.set(it.keyId, existing)
    }
    return result
  }

  fun getReport(
    projectEntity: Project,
    taskNumber: Long,
  ): List<TaskPerUserReportView> {
    return taskRepository.perUserReport(
      projectEntity.id,
      taskNumber,
      projectEntity.baseLanguage!!.id,
    )
  }

  private fun getOnlyProjectKeys(
    projectId: Long,
    languageId: Long,
    type: TaskType,
    keys: Collection<Long>,
    filters: TranslationScopeFilters,
  ): MutableSet<Long> {
    return taskRepository.getKeysWithoutConflicts(
      projectId,
      languageId,
      type.toString(),
      keys,
      filters,
    ).toMutableSet()
  }

  private fun checkAssignees(
    assignees: MutableSet<Long>,
    projectId: Long,
  ): MutableSet<UserAccount> {
    return assignees.map {
      val permission = securityService.getProjectPermissionScopesNoApiKey(projectId, it)
      if (permission.isNullOrEmpty()) {
        throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)
      }
      entityManager.getReference(UserAccount::class.java, it)
    }.toMutableSet()
  }

  private fun checkLanguage(
    language: Long,
    projectId: Long,
  ): Language {
    val allLanguages = languageService.findAll(projectId).associateBy { it.id }
    if (allLanguages[language] == null) {
      throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
    } else {
      return entityManager.getReference(Language::class.java, language)
    }
  }

  private fun getTasksWithScope(tasks: Collection<Task>): List<TaskWithScopeView> {
    val scopes = taskRepository.getTasksScopes(tasks)
    return tasks.map { task ->
      val scope = scopes.find { it.taskId == task.id }!!
      TaskWithScopeView(
        project = task.project,
        number = task.number,
        name = task.name,
        description = task.description,
        type = task.type,
        language = task.language,
        dueDate = task.dueDate,
        assignees = task.assignees,
        keys = task.keys,
        author = task.author!!,
        createdAt = task.createdAt,
        state = task.state,
        closedAt = task.closedAt,
        totalItems = scope.totalItems,
        doneItems = scope.doneItems,
        baseWordCount = scope.baseWordCount,
        baseCharacterCount = scope.baseCharacterCount,
      )
    }
  }

  private fun findByNumber(
    projectId: Long,
    taskNumber: Long,
  ): Task {
    return taskRepository.findByNumber(projectId, taskNumber)
      ?: throw NotFoundException(Message.TASK_NOT_FOUND)
  }

  private fun findTaskKey(
    taskId: Long,
    keyId: Long,
  ): TaskKey {
    return taskKeyRepository.findByTaskIdAndKeyId(taskId, keyId)
      ?: throw NotFoundException(Message.TASK_NOT_FOUND)
  }

  private fun getTaskWithScope(task: Task): TaskWithScopeView {
    val result = getTasksWithScope(listOf(task))
    if (result.isNotEmpty()) {
      return result.first()
    } else {
      throw NotFoundException(Message.TASK_NOT_FOUND)
    }
  }

  fun getExcelFile(
    project: Project,
    taskNumber: Long,
  ): ByteArray {
    val task = getTask(project.id, taskNumber)
    val report = getReport(project, taskNumber)

    val workbook = TaskReportHelper(task, report).generateExcelReport()

    // Write the workbook to a byte array output stream
    val byteArrayOutputStream = ByteArrayOutputStream()
    workbook.use { wb ->
      wb.write(byteArrayOutputStream)
    }

    val byteArray = byteArrayOutputStream.toByteArray()
    return byteArray
  }

  private fun getTaskKeyAuthorByState(done: Boolean): UserAccount? {
    if (done) {
      return entityManager.getReference(
        UserAccount::class.java,
        authenticationFacade.authenticatedUser.id,
      )
    }
    return null
  }
}
