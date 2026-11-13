package io.tolgee.service.notification.activity

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.language.LanguageService
import io.tolgee.service.notification.NotificationEligibility
import io.tolgee.service.notification.NotificationProjectPreferenceService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ProjectMemberPermissionProvider
import org.springframework.stereotype.Component

data class RecipientNotification(
  val userId: Long,
  val type: NotificationType,
  val entityIds: List<Long>,
)

class ProjectMember(
  val userId: Long,
  val permission: ComputedPermissionDto,
  val followedLanguageIds: Set<Long>?,
)

class ProjectMembers(
  val nonBaseLanguageIds: List<Long>,
  val members: List<ProjectMember>,
)

@Component
class NotificationRecipientResolver(
  private val projectMemberPermissionProvider: ProjectMemberPermissionProvider,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val tolgeeProperties: TolgeeProperties,
  private val projectPreferenceService: NotificationProjectPreferenceService,
) {
  fun loadProjectMembers(
    projectId: Long,
    baseLanguageId: Long,
  ): ProjectMembers? {
    if (projectService.findDto(projectId) == null) return null
    val members = projectMemberPermissionProvider.findMembers(projectId)
    val nonBaseLanguageIds = languageService.findAll(projectId).map { it.id }.filter { it != baseLanguageId }
    val followed = projectPreferenceService.followedLanguages(projectId, members, nonBaseLanguageIds)
    return ProjectMembers(
      nonBaseLanguageIds = nonBaseLanguageIds,
      members =
        members.map {
          ProjectMember(
            userId = it.member.id,
            permission = it.permission,
            followedLanguageIds = followed[it.member.id]?.languageIds,
          )
        },
    )
  }

  fun resolve(
    revision: ClassifiedRevision,
    projectMembers: ProjectMembers? = loadProjectMembers(revision.projectId, revision.baseLanguageId),
  ): List<RecipientNotification> {
    if (revision.items.isEmpty() || projectMembers == null) return emptyList()
    return projectMembers.members.filter { it.userId != revision.authorId }.flatMap { member ->
      revision.items.mapNotNull { (type, byLanguage) ->
        entitiesFor(type, byLanguage, member, projectMembers.nonBaseLanguageIds)
          .takeIf { it.isNotEmpty() }
          ?.let { RecipientNotification(member.userId, type, it) }
      }
    }
  }

  private fun entitiesFor(
    type: NotificationType,
    byLanguage: Map<Long?, List<Long>>,
    member: ProjectMember,
    nonBaseLanguageIds: List<Long>,
  ): List<Long> {
    val languageIds = byLanguage.keys.filterNotNull()
    val actionable =
      NotificationEligibility.actionableLanguageIds(
        type = type,
        permission = member.permission,
        languageIds = languageIds,
        nonBaseLanguageIds = nonBaseLanguageIds,
        followedLanguageIds = member.followedLanguageIds,
      ) ?: return emptyList()
    return cap(listOf(byLanguage[null].orEmpty()) + actionable.map { byLanguage[it].orEmpty() })
  }

  private fun cap(idsByLanguage: List<List<Long>>): List<Long> {
    val cap = tolgeeProperties.notifications.entityCap
    val result = LinkedHashSet<Long>()
    val longest = idsByLanguage.maxOfOrNull { it.size } ?: 0
    for (index in 0 until longest) {
      for (ids in idsByLanguage) {
        if (result.size >= cap) return result.toList()
        ids.getOrNull(index)?.let { result.add(it) }
      }
    }
    return result.toList()
  }
}
