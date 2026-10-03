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

@Component
class NotificationRecipientResolver(
  private val userAccountService: UserAccountService,
  private val permissionService: PermissionService,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun resolve(revision: ClassifiedRevision): List<RecipientNotification> {
    if (revision.items.isEmpty()) return emptyList()
    val project = projectService.findDto(revision.projectId) ?: return emptyList()
    val organizationBase = permissionService.find(organizationId = project.organizationOwnerId) ?: return emptyList()
    val nonBaseLanguageIds =
      languageService.findAll(revision.projectId).map { it.id }.filter { it != revision.baseLanguageId }

    val members =
      userAccountService
        .getAllInProject(revision.projectId, Pageable.unpaged(), search = null, exceptUserId = revision.authorId)
        .content

    return members.flatMap { member ->
      val permission =
        permissionService.computeProjectPermission(
          organizationRole = member.organizationRole,
          organizationBasePermission = organizationBase,
          directPermission = member.directPermission,
          asScopedCredential = false,
        )
      revision.items.mapNotNull { (type, byLanguage) ->
        entitiesFor(type, byLanguage, permission, nonBaseLanguageIds)
          .takeIf { it.isNotEmpty() }
          ?.let { RecipientNotification(member.id, type, it) }
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
