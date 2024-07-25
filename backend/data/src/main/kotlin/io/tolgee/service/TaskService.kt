package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.task.*
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import io.tolgee.model.key.Key
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskId
import io.tolgee.model.task.TaskTranslation
import io.tolgee.model.task.TaskTranslationId
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.KeysScopeView
import io.tolgee.model.views.TaskWithScopeView
import io.tolgee.repository.TaskRepository
import io.tolgee.repository.TaskTranslationRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.*

@Component
class TaskService(
  private val taskRepository: TaskRepository,
  private val entityManager: EntityManager,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val taskTranslationRepository: TaskTranslationRepository,
  private val translationService: TranslationService,
  private val authenticationFacade: AuthenticationFacade,
  private val translationRepository: TranslationRepository,
  @Lazy
  @Autowired
  private val taskService: TaskService,
) {
  fun getAllPaged(
    project: Project,
    pageable: Pageable,
    search: String?,
    filters: TaskFilters
  ): Page<TaskWithScopeView> {
    val pagedTasks = taskRepository.getAllByProjectId(project.id, pageable, search, filters)
    val withPrefetched = taskRepository.getByIdsWithAllPrefetched(pagedTasks.content)
    return PageImpl(getTasksWithScope(withPrefetched), pageable, pagedTasks.totalElements)
  }

  fun getUserTasksPaged(
    userId: Long,
    pageable: Pageable,
    search: String?
  ): Page<TaskWithScopeView> {
    val pagedTasks = taskRepository.getAllByAssignee(userId, pageable, search)
    val withPrefetched = taskRepository.getByIdsWithAllPrefetched(pagedTasks.content)
    return PageImpl(getTasksWithScope(withPrefetched), pageable, pagedTasks.totalElements)
  }

  @Transactional
  fun createMultipleTasks(project: Project, dtos: Collection<CreateTaskRequest>) {
    dtos.forEach {
      createTask(project, it)
    }
  }

  @Transactional
  fun createTask(
    project: Project,
    dto: CreateTaskRequest,
  ): TaskWithScopeView {
    var lastErr = DataIntegrityViolationException("Error")
    repeat(100) {
      // necessary for proper transaction creation
      try {
        val task = taskService.createTaskInTransaction(project, dto)
        entityManager.flush()
        return getTasksWithScope(listOf(task)).first()
      } catch (e: DataIntegrityViolationException) {
        lastErr = e
      }
    }
    throw lastErr
  }

  @Transactional()
  fun createTaskInTransaction(
    project: Project,
    dto: CreateTaskRequest,
  ): Task {
    // Find the maximum ID for the given project
    val lastTask = taskRepository.findByProjectOrderByIdDesc(project).firstOrNull()
    val newId = (lastTask?.id ?: 0L) + 1

    val language = checkLanguage(dto.languageId!!, project)
    val assignees = checkAssignees(dto.assignees ?: mutableSetOf(), project)
    val keys = getOnlyProjectKeys(project, dto.languageId!!, dto.type, dto.keys ?: mutableSetOf())

    val translations = getOrCreateTranslations(language.id, keys)

    val task = Task()

    task.id = newId
    task.project = project
    task.name = dto.name
    task.type = dto.type
    task.description = dto.description
    task.dueDate = dto.dueDate?.let { Date(it) }
    task.language = language
    task.assignees = assignees
    task.author = entityManager.getReference(UserAccount::class.java, authenticationFacade.authenticatedUser.id)
    task.createdAt = Date()
    task.state = dto.state ?: TaskState.IN_PROGRESS
    taskRepository.saveAndFlush(task)

    val taskTranslations = translations.map { TaskTranslation(task, it) }.toMutableSet()
    task.translations = taskTranslations
    taskTranslationRepository.saveAll(taskTranslations)

    return task
  }

  @Transactional
  fun updateTask(
    projectEntity: Project,
    taskId: Long,
    dto: UpdateTaskRequest,
  ): TaskWithScopeView {
    val task =
      taskRepository.findById(TaskId(projectEntity, taskId)).or {
        throw NotFoundException(Message.TASK_NOT_FOUND)
      }.get()

    dto.name?.let {
      task.name = it
    }

    dto.description?.let {
      task.description = it
    }

    dto.dueDate?.let {
      if (it < 0L) {
        task.dueDate = null
      } else {
        task.dueDate = Date(it)
      }
    }

    dto.assignees?.let {
      task.assignees = checkAssignees(dto.assignees!!, projectEntity)
    }

    dto.state?.let {
      task.state = it
    }

    taskRepository.saveAndFlush(task)

    return getTasksWithScope(listOf(task)).first()
  }

  @Transactional
  fun deleteTask(
    projectEntity: Project,
    taskId: Long,
  ) {
    val taskComposedId = TaskId(projectEntity, taskId)
    taskTranslationRepository.deleteByTask(entityManager.getReference(Task::class.java, taskComposedId))
    taskRepository.deleteById(taskComposedId)
  }

  @Transactional
  fun getTask(
    projectEntity: Project,
    taskId: Long,
  ): TaskWithScopeView {
    val taskComposedId = TaskId(projectEntity, taskId)
    val task = taskRepository.getReferenceById(taskComposedId)
    return getTasksWithScope(listOf(task)).first()
  }

  @Transactional
  fun updateTaskKeys(
    projectEntity: Project,
    taskId: Long,
    dto: UpdateTaskKeysRequest,
  ) {
    val task =
      taskRepository.findById(TaskId(projectEntity, taskId)).or {
        throw NotFoundException(Message.TASK_NOT_FOUND)
      }.get()

    dto.removeKeys?.let { toRemove ->
      val translationsToRemove =
        translationService
          .getLanguageTrasnlationsByIds(task.language.id, toRemove)
          .map { it.id }
      val taskKeysToRemove =
        task.translations.filter {
          translationsToRemove.contains(
            it.translation.id,
          )
        }.toMutableSet()
      task.translations = task.translations.subtract(taskKeysToRemove).toMutableSet()
      taskTranslationRepository.deleteAll(taskKeysToRemove)
    }

    dto.addKeys?.let { toAdd ->
      val translationsToAdd = getOrCreateTranslations(task.language.id, toAdd)
      val translationIdsToAdd = translationsToAdd.map { it.id }.toMutableSet()
      val existingTranslations = task.translations.map { it.translation.id }.toMutableSet()
      val nonExistingTranslationIds = translationIdsToAdd.subtract(existingTranslations).toMutableSet()
      val taskTranslationsToAdd = translationsToAdd
        .filter { nonExistingTranslationIds.contains(it.id) }
        .map { TaskTranslation(task, it) }
      task.translations = task.translations.union(taskTranslationsToAdd).toMutableSet()
      taskTranslationRepository.saveAll(taskTranslationsToAdd)
    }
  }

  @Transactional
  fun updateTaskKey(
    projectEntity: Project,
    taskId: Long,
    keyId: Long,
    dto: UpdateTaskKeyRequest,
  ) {
    val task =
      taskRepository.findById(TaskId(projectEntity, taskId)).or {
        throw NotFoundException(Message.TASK_NOT_FOUND)
      }.get()

    val translation =
      translationRepository.findOneByKeyAndLanguageId(
        entityManager.getReference(Key::class.java, keyId),
        task.language.id,
      ).or {
        throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
      }.get()

    val taskTranslation =
      taskTranslationRepository.findById(TaskTranslationId(task, translation)).or {
        throw NotFoundException(Message.TASK_NOT_FOUND)
      }.get()

    if (dto.done == true) {
      taskTranslation.author =
        entityManager.getReference(
          UserAccount::class.java,
          authenticationFacade.authenticatedUser.id,
        )
    } else {
      taskTranslation.author = null
    }
    taskTranslation.done = dto.done ?: false
    taskTranslationRepository.save(taskTranslation)
  }

  @Transactional
  fun calculateScope(
    projectEntity: Project,
    dto: CalculateScopeRequest,
  ): KeysScopeView {
    val language = languageService.get(dto.language, projectEntity.id)
    val relevantKeys =
      taskRepository.getKeysWithoutTask(
        projectEntity.id,
        language.id,
        dto.type.toString(),
        dto.keys!!,
      )
    return taskRepository.calculateScope(
      projectEntity.id,
      projectEntity.baseLanguage!!.id,
      relevantKeys,
    )
  }

  private fun getOnlyProjectKeys(
    project: Project,
    languageId: Long,
    type: TaskType,
    keys: Collection<Long>,
  ): MutableSet<Long> {
    return taskRepository.getKeysWithoutTask(
      project.id,
      languageId,
      type.toString(),
      keys,
    ).toMutableSet()
  }

  private fun checkAssignees(
    assignees: MutableSet<Long>,
    project: Project,
  ): MutableSet<UserAccount> {
    return assignees.map {
      val permission = securityService.getProjectPermissionScopesNoApiKey(project.id, it)
      if (permission.isNullOrEmpty()) {
        throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)
      }
      entityManager.getReference(UserAccount::class.java, it)
    }.toMutableSet()
  }

  private fun checkLanguage(
    language: Long,
    project: Project,
  ): Language {
    val allLanguages = languageService.findAll(project.id).associateBy { it.id }
    if (allLanguages[language] == null) {
      throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
    } else {
      return entityManager.getReference(Language::class.java, language)
    }
  }

  private fun getOrCreateTranslations(
    languageId: Long,
    keyIds: Collection<Long>,
  ): MutableList<Translation> {
    val translations = translationService.getLanguageTrasnlationsByIds(languageId, keyIds)
    val translationsHashMap = translations.associateBy({ it.key.id }, { it })

    val allTranslations = mutableListOf<Translation>()
    val newTranslations = mutableListOf<Translation>()

    keyIds.forEach {
      var translation = translationsHashMap.get(it)
      if (translation == null) {
        translation =
          Translation(
            text = null,
            key = entityManager.getReference(Key::class.java, it),
            language = entityManager.getReference(Language::class.java, languageId),
          )
        newTranslations.add(translation)
      }
      allTranslations.add(translation)
    }

    translationService.saveAll(newTranslations)
    entityManager.flush()
    return allTranslations
  }

  private fun getTasksWithScope(tasks: Collection<Task>): List<TaskWithScopeView> {
    val scopes = taskRepository.getTasksScopes(tasks)
    return tasks.map { task ->
      val scope = scopes.find { it.taskId == task.id }!!
      TaskWithScopeView(
        project = task.project,
        id = task.id,
        name = task.name,
        description = task.description,
        type = task.type,
        language = task.language,
        dueDate = task.dueDate,
        assignees = task.assignees,
        translations = task.translations,
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
}
