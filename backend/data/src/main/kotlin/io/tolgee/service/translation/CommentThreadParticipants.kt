package io.tolgee.service.translation

import io.tolgee.model.enums.Scope
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component

data class MentionableUser(
  val id: Long,
  val name: String?,
  val avatarHash: String?,
  val inThread: Boolean = false,
)

@Component
class CommentThreadParticipants(
  private val jdbcTemplate: JdbcTemplate,
  @Lazy
  private val permissionService: PermissionService,
  @Lazy
  private val projectService: ProjectService,
) {
  fun find(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    exceptUserId: Long,
    search: String?,
  ): List<MentionableUser> {
    val authors =
      jdbcTemplate.query(
        """
        select ua.id, ua.name, ua.avatar_hash
        from translation_comment c
        join translation t on t.id = c.translation_id
        join key k on k.id = t.key_id
        join user_account ua on ua.id = c.author_id
        where k.project_id = ? and t.key_id = ? and t.language_id = ? and ua.id <> ?
          and ua.deleted_at is null and ua.disabled_at is null
          and (cast(? as text) is null or lower(ua.name) like lower('%' || cast(? as text) || '%'))
        group by ua.id, ua.name, ua.avatar_hash
        order by max(c.created_at) desc, ua.id
        limit ?
        """,
        { rs, _ -> MentionableUser(rs.getLong(1), rs.getString(2), rs.getString(3), inThread = true) },
        projectId,
        keyId,
        languageId,
        exceptUserId,
        search,
        search,
        MAX_PARTICIPANTS,
      )
    return authors.filter { canViewLanguage(projectId, it.id, languageId) }
  }

  fun findAmong(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    userIds: Collection<Long>,
  ): List<MentionableUser> {
    if (userIds.isEmpty()) return emptyList()
    val authors =
      jdbcTemplate.query(
        """
        select distinct ua.id, ua.name, ua.avatar_hash
        from translation_comment c
        join translation t on t.id = c.translation_id
        join key k on k.id = t.key_id
        join user_account ua on ua.id = c.author_id
        where k.project_id = ? and t.key_id = ? and t.language_id = ? and ua.id = any(?)
          and ua.deleted_at is null and ua.disabled_at is null
        """,
        PreparedStatementSetter { ps ->
          ps.setLong(1, projectId)
          ps.setLong(2, keyId)
          ps.setLong(3, languageId)
          ps.setArray(4, ps.connection.createArrayOf("bigint", userIds.toTypedArray()))
        },
        RowMapper { rs, _ -> MentionableUser(rs.getLong(1), rs.getString(2), rs.getString(3), inThread = true) },
      )
    return authors.filter { canViewLanguage(projectId, it.id, languageId) }
  }

  private fun canViewLanguage(
    projectId: Long,
    userId: Long,
    languageId: Long,
  ): Boolean {
    val project = projectService.findDto(projectId) ?: return false
    val permission = permissionService.getProjectPermissionData(project, userId).computedPermissions
    return Scope.TRANSLATIONS_VIEW in permission.expandedScopes &&
      permission.filterViewPermitted(listOf(languageId)).isNotEmpty()
  }

  companion object {
    private const val MAX_PARTICIPANTS = 50
  }
}
