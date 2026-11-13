package io.tolgee.service.notification

import io.tolgee.model.notifications.NotificationType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.activity.NotificationChangeQueries
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class NotificationLinkResolver(
  private val jdbcTemplate: JdbcTemplate,
  private val entitiesQueryService: NotificationEntitiesQueryService,
  private val changeQueries: NotificationChangeQueries,
  private val authenticationFacade: AuthenticationFacade,
) {
  fun keyIdsFor(
    projectId: Long,
    notificationId: Long?,
  ): Set<Long>? {
    notificationId ?: return null
    val userId = authenticationFacade.authenticatedUserOrNull?.id ?: return emptySet()
    val notification = findOwn(notificationId, userId, projectId) ?: return emptySet()
    val languageIds = entitiesQueryService.getLanguages(listOf(notificationId))[notificationId].orEmpty().map { it.id }
    return changeQueries
      .keyIds(projectId, notification.type, notification.revisionIds, languageIds, MAX_KEYS)
      .toSet()
  }

  private class OwnNotification(
    val type: NotificationType,
    val revisionIds: List<Long>,
  )

  private fun findOwn(
    notificationId: Long,
    userId: Long,
    projectId: Long,
  ): OwnNotification? =
    jdbcTemplate
      .query(
        """
        select n.type, n.activity_revision_ids
        from notification n
        where n.id = ? and n.user_id = ? and n.project_id = ? and n.email_only = false
        """,
        { rs, _ ->
          OwnNotification(
            type = NotificationType.valueOf(rs.getString(1)),
            revisionIds = (rs.getArray(2)?.array as? Array<*>)?.map { (it as Number).toLong() }.orEmpty(),
          )
        },
        notificationId,
        userId,
        projectId,
      ).firstOrNull()

  companion object {
    // The ids are bound one parameter each in the translation view query; PostgreSQL allows at most 65535.
    private const val MAX_KEYS = 10_000
  }
}
