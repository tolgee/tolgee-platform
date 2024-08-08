package io.tolgee.repository

import io.tolgee.dtos.request.task.TaskFilters
import io.tolgee.dtos.request.task.TranslationScopeFilters
import io.tolgee.model.Project
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskId
import io.tolgee.model.views.KeysScopeView
import io.tolgee.model.views.TaskPerUserReportView
import io.tolgee.model.views.TaskScopeView
import io.tolgee.model.views.TranslationToTaskView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

const val TASK_SEARCH = """
    (
        cast(:search as text) is null
        or lower(tk.name) like lower(concat('%', cast(:search as text),'%'))
    )
"""

const val TASK_FILTERS = """
    (
        :#{#filters.filterNotState} is null
        or tk.state not in :#{#filters.filterNotState}
    )
    and (
        :#{#filters.filterState} is null
        or tk.state in :#{#filters.filterState}
    )
    and (
        :#{#filters.filterType} is null
        or tk.type in :#{#filters.filterType}
    )
    and (
        :#{#filters.filterId} is null
        or tk.id in :#{#filters.filterId}
    )
    and (
        :#{#filters.filterNotId} is null
        or tk.id not in :#{#filters.filterNotId}
    )
    and (
        :#{#filters.filterProject} is null
        or tk.project.id in :#{#filters.filterProject}
    )
    and (
        :#{#filters.filterNotProject} is null
        or tk.project.id not in :#{#filters.filterNotProject}
    )
    and (
        :#{#filters.filterLanguage} is null
        or tk.language.id in :#{#filters.filterLanguage}
    )
    and (
        :#{#filters.filterAssignee} is null
        or exists (
            select 1
            from tk.assignees u
            where u.id in :#{#filters.filterAssignee}
        )
    )
    and (
        :#{#filters.filterTranslation} is null
        or exists (
            select 1
            from tk.translations tt
            where tt.translation.id in :#{#filters.filterTranslation}
        )
    )
"""

const val TRANSLATION_SCOPE_FILTERS = """
    (
      :#{#filters.filterState} is null
      or COALESCE(t.state, 0) in :#{#filters.filterStateOrdinal}
    ) and (
      COALESCE(:#{#filters.filterOutdated}, false) = false
      or COALESCE(t.outdated, false) = :#{#filters.filterOutdated}
    ) and (
      COALESCE(:#{#filters.filterNotOutdated}, false) = false
      or COALESCE(t.outdated, false) != :#{#filters.filterNotOutdated}
    )
"""

@Repository
interface TaskRepository : JpaRepository<Task, TaskId> {
  @Query(
    """
     select tk
     from Task tk
     where
        tk.project.id = :projectId
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
     select tk
     from Task tk
        right join tk.assignees u on u.id = :userId
     where $TASK_SEARCH
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
     select 
        tt.translation.id as translationId,
        t.id as taskId,
        tt.done as taskDone,
        CASE WHEN u.id IS NULL THEN FALSE ELSE TRUE END as taskAssigned,
        t.type as taskType
     from Task t
        right join t.translations tt on tt.translation.id in :translationIds
        left join t.assignees u on u.id = :currentUserId
     where t.state = 'IN_PROGRESS'
     order by t.type desc, t.id desc
    """,
  )
  fun getByTranslationId(
    currentUserId: Long,
    translationIds: Collection<Long>,
  ): List<TranslationToTaskView>

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
                join task_translation on (translation.id = task_translation.translation_id)
                join task on (task_translation.task_id = task.id and task_translation.task_project_id = :projectId)
            where task.type = :taskType
                and task.language_id = :languageId
                and task.state = 'IN_PROGRESS'
          ) as task on task.key_id = key.id
          left join translation t on t.key_id = key.id and t.language_id = :languageId
      where key.project_id = :projectId
          and key.id in :keyIds
          and task IS NULL
          and $TRANSLATION_SCOPE_FILTERS
    """,
  )
  fun getKeysWithoutTask(
    projectId: Long,
    languageId: Long,
    taskType: String,
    keyIds: Collection<Long>,
    filters: TranslationScopeFilters = TranslationScopeFilters(),
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
          left join t.key k
          left join k.translations bt on (bt.language.id = p.baseLanguage.id)
      where tk in :tasks
      group by tk.id, tk.project.id
    """,
  )
  fun getTasksScopes(tasks: Collection<Task>): List<TaskScopeView>

  @Query(
    """
      select u as user, count(t.id) as doneItems, coalesce(sum(btr.characterCount), 0) as baseCharacterCount, coalesce(sum(btr.wordCount), 0) as baseWordCount
      from Task tk
        left join tk.translations as tt
        left join tt.author as u
        left join tt.translation as t
        left join t.key as k
        left join k.translations as btr on btr.language.id = :baseLangId 
      where tk.project.id = :projectId
        and tk.id = :taskId
        and tt.done
        and u.id is not NULL
      group by u
    """,
  )
  fun perUserReport(
    projectId: Long,
    taskId: Long,
    baseLangId: Long,
  ): List<TaskPerUserReportView>
}
