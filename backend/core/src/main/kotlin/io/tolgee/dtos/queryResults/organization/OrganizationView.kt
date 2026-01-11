package io.tolgee.dtos.queryResults.organization

import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class OrganizationView(
  val id: Long,
  val name: String,
  val slug: String,
  val description: String?,
  val basePermission: BasePermissionView,
  val currentUserRole: OrganizationRoleType?,
  val avatarHash: String?,
) {
  constructor(
    id: Long,
    name: String,
    slug: String,
    description: String?,
    basePermissionScopes: Any?,
    basePermissionType: ProjectPermissionType?,
    currentUserRole: OrganizationRoleType?,
    avatarHash: String?,
  ) : this(
    id = id,
    name = name,
    slug = slug,
    description = description,
    basePermission =
      BasePermissionView(
        // this has to be there, otherwise hibernate could not map it
        _scopes = (basePermissionScopes as? Array<Scope>?),
        type = basePermissionType,
      ),
    currentUserRole = currentUserRole,
    avatarHash = avatarHash,
  )

  companion object {
    fun of(
      entity: Organization,
      currentUserRole: OrganizationRoleType?,
    ): OrganizationView {
      return OrganizationView(
        id = entity.id,
        name = entity.name,
        slug = entity.slug,
        description = entity.description,
        basePermission = BasePermissionView.of(entity.basePermission),
        currentUserRole = currentUserRole,
        avatarHash = entity.avatarHash,
      )
    }
  }
}
