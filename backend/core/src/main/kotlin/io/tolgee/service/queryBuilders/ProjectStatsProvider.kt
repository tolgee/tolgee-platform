package io.tolgee.service.queryBuilders

import io.tolgee.model.OrganizationRole_
import io.tolgee.model.Organization_
import io.tolgee.model.Permission_
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.UserAccount
import io.tolgee.model.UserAccount_
import io.tolgee.model.enums.TaskState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Tag
import io.tolgee.model.key.Tag_
import io.tolgee.model.task.Task
import io.tolgee.model.task.Task_
import io.tolgee.model.views.projectStats.ProjectStatsView
import io.tolgee.util.KotlinCriteriaBuilder
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Selection

open class ProjectStatsProvider(
  val entityManager: EntityManager,
  private val projectId: Long,
) : KotlinCriteriaBuilder<ProjectStatsView>(entityManager, ProjectStatsView::class.java) {
  private var project: Root<Project> = query.from(Project::class.java)

  fun getResult(): ProjectStatsView {
    initQuery()
    return entityManager.createQuery(query).singleResult
  }

  private fun initQuery() {
    val selection =
      mutableListOf<Selection<*>>(
        project.get(Project_.id),
        getKeyCountSelection(),
        getMemberCountSelection(),
        getTagSelection(),
        getTaskCountSelection(),
      )

    query.multiselect(selection)

    query.groupBy(project.get(Project_.id))

    query.where(cb.equal(project.get(Project_.id), projectId))
  }

  private fun getKeyCountSelection(): Selection<Long> {
    val sub = query.subquery(Long::class.java)
    val key = sub.from(Key::class.java)
    sub.where(key.get(Key_.project) equal project)
    return sub.select(cb.countDistinct(key.get(Key_.id)))
  }

  private fun getTagSelection(): Selection<Long> {
    val sub = query.subquery(Long::class.java)
    val tag = sub.from(Tag::class.java)
    sub.where(tag.get(Tag_.project) equal project)
    return sub.select(cb.coalesce(cb.countDistinct(tag.get(Tag_.id)), 0))
  }

  private fun getMemberCountSelection(): Selection<Long> {
    val sub = query.subquery(Long::class.java)
    val subProject = sub.from(Project::class.java)
    val subUserAccount = sub.from(UserAccount::class.java)
    val permissionJoin = subProject.join(Project_.permissions, JoinType.LEFT)
    val organizationJoin = subProject.join(Project_.organizationOwner, JoinType.LEFT)
    val rolesJoin = organizationJoin.join(Organization_.memberRoles, JoinType.LEFT)
    sub.where(
      project equal subProject and
        (
          subUserAccount equal permissionJoin.get(Permission_.user) or
            (subUserAccount equal rolesJoin.get(OrganizationRole_.user))
        ),
    )
    return sub.select(cb.countDistinct(subUserAccount.get(UserAccount_.id)))
  }

  private fun getTaskCountSelection(): Selection<Long> {
    val sub = query.subquery(Long::class.java)
    val task = sub.from(Task::class.java)
    sub.where(
      task.get(Task_.project) equal project
        and task.get(Task_.state).`in`(listOf(TaskState.NEW, TaskState.IN_PROGRESS)),
    )
    return sub.select(cb.coalesce(cb.countDistinct(task.get(Tag_.id)), 0))
  }
}
