package io.tolgee.service.security

import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.dtos.request.task.UserAccountFilters
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.service.project.ProjectService
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

class ProjectMemberWithPermission(
  val member: UserAccountInProjectView,
  val permission: ComputedPermissionDto,
) {
  fun canViewLanguage(languageId: Long): Boolean =
    Scope.TRANSLATIONS_VIEW in permission.expandedScopes &&
      permission.filterViewPermitted(listOf(languageId)).isNotEmpty()
}

@Component
class ProjectMemberPermissionProvider(
  private val projectService: ProjectService,
  @Lazy
  private val permissionService: PermissionService,
  @Lazy
  private val userAccountService: UserAccountService,
) {
  fun findMembers(
    projectId: Long,
    onlyUserIds: Collection<Long>? = null,
  ): List<ProjectMemberWithPermission> {
    if (onlyUserIds?.isEmpty() == true) return emptyList()
    val filters = UserAccountFilters().apply { filterId = onlyUserIds?.toList() }
    val members = userAccountService.getAllInProject(projectId, Pageable.unpaged(), search = null, filters = filters)
    return withPermissions(projectId, members.content)
  }

  fun withPermissions(
    projectId: Long,
    members: List<UserAccountInProjectView>,
  ): List<ProjectMemberWithPermission> {
    if (members.isEmpty()) return emptyList()
    val project = projectService.findDto(projectId) ?: return emptyList()
    val organizationBase = permissionService.find(organizationId = project.organizationOwnerId) ?: return emptyList()
    return members.map { member ->
      ProjectMemberWithPermission(
        member = member,
        permission =
          permissionService.computeProjectPermission(
            organizationRole = member.organizationRole,
            organizationBasePermission = organizationBase,
            directPermission = member.directPermission,
            asScopedCredential = false,
          ),
      )
    }
  }
}
