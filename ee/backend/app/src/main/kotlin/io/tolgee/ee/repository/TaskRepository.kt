package io.tolgee.ee.repository

import io.tolgee.ee.data.task.TaskFilters
import io.tolgee.ee.data.task.TranslationScopeFilters
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.model.views.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

private const val TASK_SEARCH = """
    (
        cast(:search as text) is null
        or lower(tk.name) like lower(concat('%', cast(:search as text),'%'))
    )
"""

private const val TASK_FILTERS = """
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
        or tk.number in :#{#filters.filterId}
    )
    and (
        :#{#filters.filterNotId} is null
        or tk.number not in :#{#filters.filterNotId}
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
        :#{#filters.filterKey} is null
        or exists (
            select 1
            from tk.keys tt
            where element(tt).key.id in :#{#filters.filterKey}
        )
    )
    and (
        tk.state != 'DONE'
        or :#{#filters.filterDoneMinClosedAt} is null
        or tk.closedAt > :#{#filters.filterDoneMinClosedAt}
    )
"""

@Repository
interface TaskRepository : JpaRepository<Task, Long> {
  @Query(
    """
     select tk
     from Task tk
        left join tk.language l
     where
        l.deletedAt is null
        and tk.project.id = :projectId
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
        join tk.assignees u on u.id = :userId
        left join tk.language l
     where l.deletedAt is null
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
    nativeQuery = true,
    value = """
     select distinct on (l.id, tt.key_id)
        tt.key_id as keyId,
        l.id as languageId,
        l.tag as languageTag,
        t.number as taskNumber,
        tt.done as taskDone,
        CASE WHEN u.id IS NULL THEN FALSE ELSE TRUE END as taskAssigned,
        t.type as taskType
     from task t
        join task_key tt on (t.id = tt.task_id)
        left join task_assignees ta on (ta.tasks_id = t.id and ta.assignees_id = :currentUserId)
        left join user_account u on ta.assignees_id = u.id
        left join language l on (t.language_id = l.id)
     where
        tt.key_id in :keyIds
        and l.deleted_at is null
        and (t.state = 'IN_PROGRESS' or t.state = 'NEW')
     order by l.id, tt.key_id, t.type desc, t.id desc
    """,
  )
  fun getByKeyId(
    currentUserId: Long,
    keyIds: Collection<Long>,
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

  @Query(
    """
      select t
      from Task t
      where
        t.project = :project
      order by t.number desc
    """,
  )
  fun findByProjectOrderByNumberDesc(project: Project): List<Task>

  @Query(
    nativeQuery = true,
    value = """
      select key.id
      from key
          left join translation t on t.key_id = key.id and t.language_id = :languageId
      where key.project_id = :projectId
          and key.id in :keyIds
          and (
            COALESCE(t.state, 0) in :#{#filters.filterStateOrdinal} -- item fits the filter
            or (
                -- item fits the filter
                :#{#filters.filterOutdated} = true 
                and COALESCE(t.outdated, false) = true
            ) or (
              -- no filter is applied
              COALESCE(:#{#filters.filterOutdated}, false) = false
              and :#{#filters.filterState} is null
            )
          )
    """,
  )
  fun getKeysIncludingConflicts(
    projectId: Long,
    languageId: Long,
    keyIds: Collection<Long>,
    filters: TranslationScopeFilters = TranslationScopeFilters(),
  ): List<Long>

  @Query(
    nativeQuery = true,
    value = """
      select key.id
      from key
          left join (
            select key.id as key_id from key
                join task_key on (key.id = task_key.key_id)
                join task on (task_key.task_id = task.id)
                left join language l on (task.language_id = l.id)
            where task.type = :taskType
                and task.language_id = :languageId
                and (task.state = 'IN_PROGRESS' or task.state = 'NEW')
                and l.deleted_at is null
          ) as task on task.key_id = key.id
          left join translation t on t.key_id = key.id and t.language_id = :languageId
      where key.project_id = :projectId
          and key.id in :keyIds
          and task IS NULL
          and (
            COALESCE(t.state, 0) in :#{#filters.filterStateOrdinal} -- item fits the filter
            or (
                -- item fits the filter
                :#{#filters.filterOutdated} = true 
                and COALESCE(t.outdated, false) = true
            ) or (
              -- no filter is applied
              COALESCE(:#{#filters.filterOutdated}, false) = false
              and :#{#filters.filterState} is null
            )
          )
    """,
  )
  fun getKeysWithoutConflicts(
    projectId: Long,
    languageId: Long,
    taskType: String,
    keyIds: Collection<Long>,
    filters: TranslationScopeFilters = TranslationScopeFilters(),
  ): List<Long>

  @Query(
    """
      select k.id
      from Key k
          left join k.tasks tt
          left join tt.task t
      where k.project.id = :projectId and t.number = :taskNumber
    """,
  )
  fun getTaskKeys(
    projectId: Long,
    taskNumber: Long,
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
  ): KeysScopeSimpleView

  @Query(
    value = """
      select
          tk.id as taskId,
          count(k.id) as totalItems,
          coalesce(sum(case when tt.done then 1 else 0 end), 0) as doneItems,
          coalesce(sum(bt.characterCount), 0) as baseCharacterCount,
          coalesce(sum(bt.wordCount), 0) as baseWordCount
      from Task tk
          left join tk.project p
          left join tk.keys tt
          left join Key k on element(tt).key.id = k.id
          left join k.translations bt on (bt.language.id = p.baseLanguage.id)
      where tk in :tasks
      group by tk.id, tk.project.id
    """,
  )
  fun getTasksScopes(tasks: Collection<Task>): List<TaskScopeView>

  @Query(
    """
      select u as user, count(k.id) as doneItems, coalesce(sum(btr.characterCount), 0) as baseCharacterCount, coalesce(sum(btr.wordCount), 0) as baseWordCount
      from Task tk
        left join tk.keys as tt
        left join tt.author as u
        left join Key k on element(tt).key.id = k.id
        left join k.translations as btr on btr.language.id = :baseLangId 
      where tk.project.id = :projectId
        and tk.number = :taskNumber
        and tt.done
        and u.id is not NULL
      group by u
    """,
  )
  fun perUserReport(
    projectId: Long,
    taskNumber: Long,
    baseLangId: Long,
  ): List<TaskPerUserReportView>

  @Query(
    """
      select u
      from UserAccount u
        join u.tasks tk
      where tk.number = :taskNumber
        and tk.project.id = :projectId
        and u.id = :userId
    """,
  )
  fun findAssigneeById(
    projectId: Long,
    taskNumber: Long,
    userId: Long,
  ): List<UserAccount>

  @Query(
    """
      select u
      from UserAccount u
        join u.tasks tk
        join tk.keys tt
      where (:type is NULL OR tk.type = :type)
        and tk.language.id = :languageId
        and (tk.state = 'IN_PROGRESS' or tk.state = 'NEW')
        and u.id = :userId
        and element(tt).key.id = :keyId
    """,
  )
  fun findAssigneeByKey(
    keyId: Long,
    languageId: Long,
    userId: Long,
    type: TaskType? = null,
  ): List<UserAccount>

  @Query(
    """
      select distinct t.number
      from Task t
        join t.keys tt
        join Task at on (at.number = :taskNumber and at.project.id = :projectId)
        join at.keys att
        join Key k on (element(att).key.id = k.id and element(tt).key.id = k.id)
      where (t.number > :taskNumber or t.type != at.type)
        and t.language = at.language
        and t.type >= at.type
        and (t.state = 'IN_PROGRESS' or t.state = 'NEW')
    """,
  )
  fun getBlockingTaskNumbers(
    projectId: Long,
    taskNumber: Long,
  ): List<Long>

  @Query(
    """
      from Task t
        left join fetch t.assignees
        left join fetch t.project
        left join fetch t.language
      where t.number = :taskNumber
        and t.project.id = :projectId
    """,
  )
  fun findByNumber(
    projectId: Long,
    taskNumber: Long,
  ): Task?
}
