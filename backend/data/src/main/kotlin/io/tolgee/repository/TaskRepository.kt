package io.tolgee.repository

import io.tolgee.model.Project
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : JpaRepository<Task, TaskId> {
  @Query(
    """
     select t
     from Task t
     where t.project.id = :projectId
    """,
  )
  fun getAllByProjectId(
    projectId: Long,
    pageable: Pageable,
  ): Page<Task>

  @Query(
    """
     select t
     from Task t
        left join t.assignees u
     where u.id = :userId
    """,
  )
  fun getAllByAssignee(
    userId: Long,
    pageable: Pageable,
  ): Page<Task>

  @Query(
    """
      select t
      from Task t
        left join fetch t.assignees
        left join fetch t.author
        left join fetch t.project
        left join fetch t.language
      where t.id in :ids
    """
  )
  fun getByIdsWithAllPrefetched(ids: Collection<Long>): List<Task>

  fun findByProjectOrderByIdDesc(project: Project): List<Task>

  @Query(
    nativeQuery = true,
    value = """
      select key.id
      from key
          left join (
            select translation.key_id as key_id from translation
                left join task_translation on (translation.id = task_translation.translation_id)
                left join task on (task_translation.task_id = task.id and task_translation.task_project_id = :projectId)
            where task.type = :taskType
                and task.language_id = :languageId
          ) as task on task.key_id = key.id
      where key.project_id = :projectId
          and key.id in :keyIds
          and task IS NULL
    """,
  )
  fun getKeysWithoutTask(
    projectId: Long,
    languageId: Long,
    taskType: String,
    keyIds: Collection<Long>,
  ): List<Long>

  @Query(
    nativeQuery = true,
    value = """
      select count(key.id) as keyCount, sum(translation.character_count) as characterCount, sum(translation.word_count) as wordCount
      from key
        left join translation on translation.key_id = key.id
      where key.project_id = :projectId
        and (translation.language_id = :baseLangId or translation.id is NULL)
        and key.id in :keyIds
    """,
  )
  fun calculateScope(
    projectId: Long,
    baseLangId: Long,
    keyIds: Collection<Long>,
  ): List<Array<Any>>

  @Query(
    value = """
          SELECT t.id
          FROM task t
          JOIN task_translation tt ON tt.task_id = t.id
          WHERE tt.translation_id = :translationId
          ORDER BY t.type ASC
          LIMIT 1
        """,
    nativeQuery = true,
  )
  fun findFirstTaskIdByTranslationId(translationId: Long): Long?
}
