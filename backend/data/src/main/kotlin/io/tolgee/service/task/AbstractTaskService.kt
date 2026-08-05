package io.tolgee.service.task

import jakarta.persistence.EntityManager

abstract class AbstractTaskService(
  protected val entityManager: EntityManager,
) : ITaskService {
  protected fun deleteNotificationsLinkedToTasks(taskIds: Collection<Long>) {
    if (taskIds.isEmpty()) return
    entityManager
      .createQuery("DELETE FROM Notification n WHERE n.linkedTask.id IN :taskIds")
      .setParameter("taskIds", taskIds)
      .executeUpdate()
  }

  override fun deleteAllByProjectId(projectId: Long) {
    val projectTaskIds = "SELECT t.id FROM Task t WHERE t.project.id = :projectId"
    entityManager
      .createQuery("DELETE FROM Notification n WHERE n.project.id = :projectId OR n.linkedTask.id IN ($projectTaskIds)")
      .setParameter("projectId", projectId)
      .executeUpdate()
    entityManager
      .createNativeQuery(
        "delete from task_assignees where tasks_id in (select id from task where project_id = :projectId)",
      ).setParameter("projectId", projectId)
      .executeUpdate()
    entityManager
      .createQuery("DELETE FROM TaskKey tk WHERE tk.task.id IN ($projectTaskIds)")
      .setParameter("projectId", projectId)
      .executeUpdate()
    entityManager
      .createQuery("DELETE FROM Task t WHERE t.project.id = :projectId")
      .setParameter("projectId", projectId)
      .executeUpdate()
  }
}
