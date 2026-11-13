package io.tolgee.service.notification

import io.tolgee.component.CurrentDateProvider
import io.tolgee.events.user.OnUserDeleted
import io.tolgee.service.security.ProjectMemberWithPermission
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Duration
import java.util.Date

enum class FollowedLanguagesState {
  DEFAULT,
  CHOSEN,
  MUTED,
}

class FollowedLanguages(
  val languageIds: Set<Long>,
  val state: FollowedLanguagesState,
)

/**
 * The languages a user follows in a project for work notifications. Until the user saves their own choice, they
 * follow a default picked from their recent work, stored with `is_default` and refreshed from time to time.
 */
@Service
class NotificationProjectPreferenceService(
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
) {
  private class StoredPreference(
    val languageIds: Set<Long>,
    val isDefault: Boolean,
    val updatedAt: Date,
  )

  /**
   * Members missing from the result follow nothing in particular: they have no language to edit or review and
   * never saved a choice, so their notifications are not narrowed.
   */
  @Transactional
  fun followedLanguages(
    projectId: Long,
    members: List<ProjectMemberWithPermission>,
    nonBaseLanguageIds: List<Long>,
  ): Map<Long, FollowedLanguages> {
    if (members.isEmpty()) return emptyMap()
    val stored = find(projectId, members.map { it.member.id })
    val chosen =
      stored
        .filterValues { !it.isDefault }
        .mapValues { (_, preference) -> chosenLanguages(preference.languageIds) }
    val needDefault =
      members
        .filter { it.member.id !in chosen }
        .associate { it.member.id to NotificationEligibility.followableLanguageIds(it.permission, nonBaseLanguageIds) }
        .filterValues { it.isNotEmpty() }
    val freshDefaults =
      needDefault.keys
        .mapNotNull { userId -> stored[userId]?.takeIf { it.isDefault && it.isFresh() }?.let { userId to it } }
        .associate { (userId, preference) -> userId to preference.languageIds }
    val staleMembers = members.filter { it.member.id in needDefault && it.member.id !in freshDefaults }
    val refreshed = refreshDefaults(projectId, staleMembers, nonBaseLanguageIds)
    val defaults =
      (freshDefaults + refreshed).mapValues { (_, ids) -> FollowedLanguages(ids, FollowedLanguagesState.DEFAULT) }
    return defaults + chosen
  }

  @Transactional
  fun set(
    userId: Long,
    projectId: Long,
    languageIds: Collection<Long>?,
  ) {
    if (languageIds == null) {
      jdbcTemplate.update(
        "delete from notification_project_preference where user_id = ? and project_id = ?",
        userId,
        projectId,
      )
      return
    }
    upsert(userId, projectId, languageIds.toSet(), isDefault = false)
  }

  /**
   * A choice left empty by the deletion was not a decision to mute the project, so it falls back to the default.
   */
  @Transactional
  fun removeLanguage(
    projectId: Long,
    languageId: Long,
  ) {
    jdbcTemplate.update(
      """
      delete from notification_project_preference
      where project_id = ? and ? = any(language_ids) and (is_default or cardinality(language_ids) = 1)
      """,
      projectId,
      languageId,
    )
    jdbcTemplate.update(
      """
      update notification_project_preference set language_ids = array_remove(language_ids, ?)
      where project_id = ? and ? = any(language_ids)
      """,
      languageId,
      projectId,
      languageId,
    )
  }

  @EventListener
  fun onUserDeleted(event: OnUserDeleted) {
    jdbcTemplate.update("delete from notification_project_preference where user_id = ?", event.userAccount.id)
  }

  private fun chosenLanguages(languageIds: Set<Long>): FollowedLanguages {
    if (languageIds.isEmpty()) return FollowedLanguages(languageIds, FollowedLanguagesState.MUTED)
    return FollowedLanguages(languageIds, FollowedLanguagesState.CHOSEN)
  }

  private fun StoredPreference.isFresh() = updatedAt.time > currentDateProvider.date.time - DEFAULT_REFRESH.toMillis()

  private fun refreshDefaults(
    projectId: Long,
    members: List<ProjectMemberWithPermission>,
    nonBaseLanguageIds: List<Long>,
  ): Map<Long, Set<Long>> {
    if (members.isEmpty()) return emptyMap()
    val recent = recentlyWorkedLanguages(projectId, members.map { it.member.id })
    return members.associate { member ->
      val userId = member.member.id
      val defaultIds = defaultLanguages(member, nonBaseLanguageIds, recent[userId].orEmpty())
      upsert(userId, projectId, defaultIds, isDefault = true)
      userId to defaultIds
    }
  }

  private fun defaultLanguages(
    member: ProjectMemberWithPermission,
    nonBaseLanguageIds: List<Long>,
    recentlyWorked: Set<Long>,
  ): Set<Long> {
    val followable = NotificationEligibility.followableLanguageIds(member.permission, nonBaseLanguageIds)
    val worked = followable.filter { it in recentlyWorked }
    if (worked.isNotEmpty()) return worked.toSet()
    val editable = NotificationEligibility.editableLanguageIds(member.permission, nonBaseLanguageIds)
    if (editable.isNotEmpty()) return editable.toSet()
    return followable.toSet()
  }

  private fun recentlyWorkedLanguages(
    projectId: Long,
    userIds: List<Long>,
  ): Map<Long, Set<Long>> =
    jdbcTemplate
      .query(
        """
        select distinct ar.author_id, (ame.describing_relations -> 'language' ->> 'entityId')::bigint
        from activity_revision ar
        join activity_modified_entity ame on ame.activity_revision_id = ar.id
        where ar.project_id = ? and ar.author_id = any(?) and ar."timestamp" >= ? and ar.batch_job_id is null
          and ame.entity_class = 'Translation'
          and (ame.modifications -> 'text' is not null or ame.modifications -> 'state' is not null)
          and ame.describing_relations -> 'language' ->> 'entityId' is not null
        """,
        PreparedStatementSetter { ps ->
          ps.setLong(1, projectId)
          ps.setArray(2, ps.connection.createArrayOf("bigint", userIds.toTypedArray()))
          ps.setTimestamp(3, Timestamp(currentDateProvider.date.time - RECENT_WORK.toMillis()))
        },
        { rs, _ -> rs.getLong(1) to rs.getLong(2) },
      ).groupBy({ it.first }, { it.second })
      .mapValues { it.value.toSet() }

  private fun find(
    projectId: Long,
    userIds: List<Long>,
  ): Map<Long, StoredPreference> =
    jdbcTemplate
      .query(
        """
        select user_id, language_ids, is_default, updated_at from notification_project_preference
        where project_id = ? and user_id = any(?)
        """,
        PreparedStatementSetter { ps ->
          ps.setLong(1, projectId)
          ps.setArray(2, ps.connection.createArrayOf("bigint", userIds.toTypedArray()))
        },
        { rs, _ ->
          rs.getLong(1) to
            StoredPreference(
              languageIds = (rs.getArray(2).array as Array<*>).map { (it as Number).toLong() }.toSet(),
              isDefault = rs.getBoolean(3),
              updatedAt = rs.getTimestamp(4),
            )
        },
      ).toMap()

  private fun upsert(
    userId: Long,
    projectId: Long,
    languageIds: Set<Long>,
    isDefault: Boolean,
  ) {
    jdbcTemplate.update(
      """
      insert into notification_project_preference (user_id, project_id, language_ids, is_default, updated_at)
      values (?, ?, ?, ?, ?)
      on conflict (user_id, project_id) do update
      set language_ids = excluded.language_ids, is_default = excluded.is_default, updated_at = excluded.updated_at
      where not excluded.is_default or notification_project_preference.is_default
      """,
      PreparedStatementSetter { ps: PreparedStatement ->
        ps.setLong(1, userId)
        ps.setLong(2, projectId)
        ps.setArray(3, ps.connection.createArrayOf("bigint", languageIds.sorted().toTypedArray()))
        ps.setBoolean(4, isDefault)
        ps.setTimestamp(5, Timestamp(currentDateProvider.date.time))
      },
    )
  }

  companion object {
    private val RECENT_WORK = Duration.ofDays(90)
    private val DEFAULT_REFRESH = Duration.ofDays(1)
  }
}
