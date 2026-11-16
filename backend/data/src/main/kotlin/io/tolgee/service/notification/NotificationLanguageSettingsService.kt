package io.tolgee.service.notification

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ProjectMemberPermissionProvider
import io.tolgee.service.security.ProjectMemberWithPermission
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class ProjectNotificationLanguages(
  val projectId: Long,
  val projectName: String,
  val availableLanguages: List<LanguageDto>,
  val followedLanguageIds: List<Long>,
  val state: FollowedLanguagesState,
)

@Service
class NotificationLanguageSettingsService(
  private val projectService: ProjectService,
  private val projectMemberPermissionProvider: ProjectMemberPermissionProvider,
  private val languageService: LanguageService,
  private val projectPreferenceService: NotificationProjectPreferenceService,
) {
  @Transactional
  fun getForUser(user: UserAccount): List<ProjectNotificationLanguages> =
    projectService
      .findAllPermitted(user)
      .mapNotNull { project -> project.id?.let { forProject(it, project.name.orEmpty(), user.id) } }
      .sortedBy { it.projectName.lowercase() }

  @Transactional
  fun set(
    userId: Long,
    projectId: Long,
    languageIds: List<Long>?,
  ) {
    val member = findMember(projectId, userId) ?: throw NotFoundException()
    val followableIds = NotificationEligibility.followableLanguageIds(member.permission, nonBaseLanguageIds(projectId))
    if (languageIds?.any { it !in followableIds } == true) {
      throw BadRequestException("notification_language_not_followable")
    }
    projectPreferenceService.set(userId, projectId, languageIds)
  }

  private fun forProject(
    projectId: Long,
    projectName: String,
    userId: Long,
  ): ProjectNotificationLanguages? {
    val member = findMember(projectId, userId) ?: return null
    val nonBase = languageService.findAll(projectId).filterNot { it.base }.sortedBy { it.tag }
    val nonBaseIds = nonBase.map { it.id }
    val followableIds = NotificationEligibility.followableLanguageIds(member.permission, nonBaseIds).toSet()
    if (followableIds.isEmpty()) return null
    val followed = projectPreferenceService.followedLanguages(projectId, listOf(member), nonBaseIds)[userId]
    return ProjectNotificationLanguages(
      projectId = projectId,
      projectName = projectName,
      availableLanguages = nonBase.filter { it.id in followableIds },
      followedLanguageIds = nonBaseIds.filter { it in followableIds && followed?.languageIds?.contains(it) == true },
      state = followed?.state ?: FollowedLanguagesState.DEFAULT,
    )
  }

  private fun findMember(
    projectId: Long,
    userId: Long,
  ): ProjectMemberWithPermission? = projectMemberPermissionProvider.findMembers(projectId, listOf(userId)).firstOrNull()

  private fun nonBaseLanguageIds(projectId: Long) =
    languageService.findAll(projectId).filterNot { it.base }.map { it.id }
}
