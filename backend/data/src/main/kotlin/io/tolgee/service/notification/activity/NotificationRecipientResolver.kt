package io.tolgee.service.notification.activity

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.model.enums.Scope
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

data class RecipientNotification(
  val userId: Long,
  val type: NotificationType,
  val entityIds: List<Long>,
)

class ProjectMember(
  val userId: Long,
  val permission: ComputedPermissionDto,
)

class ProjectMembers(
  val nonBaseLanguageIds: List<Long>,
  val members: List<ProjectMember>,
)

@Component
class NotificationRecipientResolver(
  private val userAccountService: UserAccountService,
  private val permissionService: PermissionService,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun loadProjectMembers(
    projectId: Long,
    baseLanguageId: Long,
  ): ProjectMembers? {
    val project = projectService.findDto(projectId) ?: return null
    val organizationBase = permissionService.find(organizationId = project.organizationOwnerId) ?: return null
    val members =
      userAccountService
        .getAllInProject(projectId, Pageable.unpaged(), search = null)
        .content
        .map { member ->
          ProjectMember(
            userId = member.id,
            permission =
              permissionService.computeProjectPermission(
                organizationRole = member.organizationRole,
                organizationBasePermission = organizationBase,
                directPermission = member.directPermission,
                asScopedCredential = false,
              ),
          )
        }
    return ProjectMembers(
      nonBaseLanguageIds = languageService.findAll(projectId).map { it.id }.filter { it != baseLanguageId },
      members = members,
    )
  }

  fun resolve(
    revision: ClassifiedRevision,
    projectMembers: ProjectMembers? = loadProjectMembers(revision.projectId, revision.baseLanguageId),
  ): List<RecipientNotification> {
    if (revision.items.isEmpty() || projectMembers == null) return emptyList()
    return projectMembers.members.filter { it.userId != revision.authorId }.flatMap { member ->
      revision.items.mapNotNull { (type, byLanguage) ->
        entitiesFor(type, byLanguage, member.permission, projectMembers.nonBaseLanguageIds)
          .takeIf { it.isNotEmpty() }
          ?.let { RecipientNotification(member.userId, type, it) }
      }
    }
  }

  private fun entitiesFor(
    type: NotificationType,
    byLanguage: Map<Long?, List<Long>>,
    permission: ComputedPermissionDto,
    nonBaseLanguageIds: List<Long>,
  ): List<Long> {
    val scopes = permission.expandedScopes
    val languageIds = byLanguage.keys.filterNotNull()
    val permittedLanguages =
      when (type) {
        NotificationType.KEYS_ADDED, NotificationType.SOURCE_CHANGED -> {
          if (Scope.TRANSLATIONS_EDIT !in scopes) return emptyList()
          if (permission.filterTranslatePermitted(nonBaseLanguageIds).isEmpty()) return emptyList()
          return cap(listOf(byLanguage[null].orEmpty()))
        }
        NotificationType.STRINGS_TRANSLATED -> {
          if (Scope.TRANSLATIONS_STATE_EDIT !in scopes) return emptyList()
          permission.filterStateChangePermitted(languageIds)
        }
        NotificationType.STRINGS_REVIEWED,
        NotificationType.AUTOMATICALLY_TRANSLATED,
        NotificationType.BULK_CHANGED,
        -> {
          if (Scope.TRANSLATIONS_VIEW !in scopes) return emptyList()
          permission.filterViewPermitted(languageIds)
        }
        else -> return emptyList()
      }
    return cap(permittedLanguages.map { byLanguage[it].orEmpty() })
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
