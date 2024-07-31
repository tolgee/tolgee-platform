package io.tolgee.repository

import io.tolgee.dtos.request.task.TaskFilters
import io.tolgee.model.Project
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskId
import io.tolgee.model.views.KeysScopeView
import io.tolgee.model.views.TaskScopeView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

const val TASK_SEARCH = """
    (
        cast(:search as text) is null
        or lower(t.name) like lower(concat('%', cast(:search as text),'%'))
    )
"""

const val TASK_FILTERS = """
    (
        :#{#filters.filterNotState} is null
        or t.state not in :#{#filters.filterNotState}
    )
    and (
        :#{#filters.filterState} is null
        or t.state in :#{#filters.filterState}
    )
    and (
        :#{#filters.filterType} is null
        or t.type in :#{#filters.filterType}
    )
    and (
        :#{#filters.filterId} is null
        or t.id in :#{#filters.filterId}
    )
    and (
        :#{#filters.filterNotId} is null
        or t.id not in :#{#filters.filterNotId}
    )
    and (
        :#{#filters.filterProject} is null
        or t.project.id in :#{#filters.filterProject}
    )
    and (
        :#{#filters.filterNotProject} is null
        or t.project.id not in :#{#filters.filterNotProject}
    )
    and (
        :#{#filters.filterLanguage} is null
        or t.language.id in :#{#filters.filterLanguage}
    )
    and (
        :#{#filters.filterAssignee} is null
        or exists (
            select 1
            from t.assignees u
            where u.id in :#{#filters.filterAssignee}
        )
    )
    and (
        :#{#filters.filterTranslation} is null
        or exists (
            select 1
            from t.translations tt
            where tt.translation.id in :#{#filters.filterTranslation}
        )
    )
"""

@Repository
interface TaskRepository : JpaRepository<Task, TaskId> {
  @Query(
    """
     select t
     from Task t
     where
        t.project.id = :projectId
        and $TASK_SEARCH
        and $TASK_FILTERS
    """,
  )
  fun getAllByProjectId(
    projectId: Long,
    pageable: Pageable,
    search: String?,
    filters: TaskFilters,
  ): Page<Task>

  @Query(
    """
     select t
     from Task t
        left join t.assignees u
        left join t.translations tt
     where u.id = :userId 
        and $TASK_SEARCH
        and $TASK_FILTERS
    """,
  )
  fun getAllByAssignee(
    userId: Long,
    pageable: Pageable,
    search: String?,
    filters: TaskFilters,
  ): Page<Task>

  @Query(
    """
      select t
      from Task t
        left join fetch t.assignees
        left join fetch t.author
        left join fetch t.project
        left join fetch t.language
      where t in :tasks
    """,
  )
  fun getByIdsWithAllPrefetched(tasks: Collection<Task>): List<Task>

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
    """
      select count(k.id) as keyCount, coalesce(sum(t.characterCount), 0) as characterCount, coalesce(sum(t.wordCount), 0) as wordCount
      from Key k
        left join k.translations as t
      where k.project.id = :projectId
        and (t.language.id = :baseLangId or t.id is NULL)
        and k.id in :keyIds
    """,
  )
  fun calculateScope(
    projectId: Long,
    baseLangId: Long,
    keyIds: Collection<Long>,
  ): KeysScopeView

  @Query(
    value = """
      select
          tk.id as taskId,
          tk.project.id as projectId,
          count(t.id) as totalItems,
          coalesce(sum(case when tt.done then 1 else 0 end), 0) as doneItems,
          coalesce(sum(bt.characterCount), 0) as baseCharacterCount,
          coalesce(sum(bt.wordCount), 0) as baseWordCount
      from Task tk
          left join tk.project p
          left join tk.translations tt
          left join tt.translation t
          left join tt.translation bt on (bt.key.id = t.key.id and bt.language.id = p.baseLanguage.id)
      where tk in :tasks
      group by tk.id, tk.project.id
    """,
  )
  fun getTasksScopes(tasks: Collection<Task>): List<TaskScopeView>
}
