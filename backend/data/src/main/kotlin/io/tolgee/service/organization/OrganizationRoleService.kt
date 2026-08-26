package io.tolgee.service.organization

import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.cacheable.UserOrganizationRoleDto
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Invitation
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.security.UserPreferencesService
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Suppress("SelfReferenceConstructorParameter")
class OrganizationRoleService(
  private val organizationRoleRepository: OrganizationRoleRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val userAccountService: UserAccountService,
  @param:Lazy
  private val permissionService: PermissionService,
  private val organizationRepository: OrganizationRepository,
  @param:Lazy
  private val userPreferencesService: UserPreferencesService,
  @param:Lazy
  private val projectService: ProjectService,
  @param:Lazy
  private val self: OrganizationRoleService,
  private val cacheManager: CacheManager,
) {
  fun canUserViewStrict(
    userId: Long,
    organizationId: Long,
  ) = this.organizationRepository.canUserView(userId, organizationId)

  fun checkUserCanViewOrPublic(organizationId: Long) {
    if (canUserViewOrPublic(authenticationFacade.authenticatedUser, organizationId)) {
      return
    }
    throw PermissionException(Message.USER_CANNOT_VIEW_THIS_ORGANIZATION)
  }

  fun canUserViewOrPublic(
    userId: Long,
    organizationId: Long,
  ): Boolean {
    val user = userAccountService.findDto(userId) ?: return false
    return canUserViewOrPublic(user, organizationId)
  }

  fun canUserViewOrPublic(
    user: UserAccountDto,
    organizationId: Long,
  ): Boolean {
    if (user.isSupporterOrAdmin()) {
      return organizationRepository.find(organizationId) != null
    }
    return canUserViewStrictOrPublic(user.id, organizationId)
  }

  fun canUserViewAtLeastMember(
    user: UserAccountDto,
    organizationId: Long,
  ): Boolean = user.isSupporterOrAdmin() || hasAnyOrganizationRole(user.id, organizationId)

  fun canUserViewStrictOrPublic(
    userId: Long,
    organizationId: Long,
  ): Boolean = canUserViewStrict(userId, organizationId) || projectService.hasPublicProjects(organizationId)

  fun checkUserCanView(organizationId: Long) {
    checkUserCanView(
      authenticationFacade.authenticatedUser,
      organizationId,
    )
  }

  private fun checkUserCanView(
    user: UserAccountDto,
    organizationId: Long,
  ) {
    if (!user.isSupporterOrAdmin() &&
      !canUserViewStrict(
        user.id,
        organizationId,
      )
    ) {
      throw PermissionException(Message.USER_CANNOT_VIEW_THIS_ORGANIZATION)
    }
  }

  /** The organization-level scopes the user holds, derived from their role (empty for a non-member). */
  fun getOrganizationScopes(
    userId: Long,
    organizationId: Long,
  ): Set<Scope> {
    return findType(userId, organizationId)?.availableScopes?.toSet() ?: emptySet()
  }

  /**
   * Throws unless the current user holds [scope] on the organization. Server admins always pass. The
   * programmatic counterpart of [io.tolgee.security.authorization.RequiresOrganizationScopes] for
   * endpoints whose organization is not on the request path (e.g. a project's owning organization).
   */
  fun checkOrganizationScope(
    organizationId: Long,
    scope: Scope,
    message: Message = Message.USER_IS_NOT_OWNER_OF_ORGANIZATION,
  ) {
    val user = authenticationFacade.authenticatedUser
    if (user.isAdmin()) {
      return
    }
    if (getOrganizationScopes(user.id, organizationId).contains(scope)) {
      return
    }
    throw PermissionException(message)
  }

  fun hasAnyOrganizationRole(
    userId: Long,
    organizationId: Long,
  ): Boolean = findType(userId, organizationId) != null

  fun find(id: Long): OrganizationRole? {
    return organizationRoleRepository.findById(id).orElse(null)
  }

  fun getType(
    userId: Long,
    organizationId: Long,
  ): OrganizationRoleType {
    return self.findType(userId, organizationId)
      ?: throw PermissionException(Message.USER_IS_NOT_MEMBER_OF_ORGANIZATION)
  }

  fun getType(organizationId: Long): OrganizationRoleType {
    return self.getType(authenticationFacade.authenticatedUser.id, organizationId)
  }

  fun findType(organizationId: Long): OrganizationRoleType? {
    return self.findType(authenticationFacade.authenticatedUser.id, organizationId)
  }

  fun findType(
    userId: Long,
    organizationId: Long,
  ): OrganizationRoleType? {
    return self.getDto(organizationId, userId).type
  }

  @Cacheable(Caches.ORGANIZATION_ROLES, key = "{#organizationId, #userId}")
  fun getDto(
    organizationId: Long,
    userId: Long,
  ): UserOrganizationRoleDto {
    val entity = organizationRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)
    return UserOrganizationRoleDto.fromEntity(userId, entity)
  }

  fun getManagedBy(userId: Long): Organization? {
    return organizationRoleRepository.findOneByUserIdAndManagedIsTrue(userId)?.organization
  }

  @CacheEvict(Caches.ORGANIZATION_ROLES, key = "{#organization.id, #user.id}")
  fun setManaged(
    user: UserAccount,
    organization: Organization,
    managed: Boolean,
  ) {
    val role =
      organizationRoleRepository.findOneByUserIdAndOrganizationId(user.id, organization.id)
        ?: throw NotFoundException(Message.USER_IS_NOT_MEMBER_OF_ORGANIZATION)
    role.managed = managed
    organizationRoleRepository.save(role)
  }

  @CacheEvict(Caches.ORGANIZATION_ROLES, key = "{#organization.id, #user.id}")
  @Transactional
  fun grantRoleToUser(
    user: UserAccount,
    organization: Organization,
    organizationRoleType: OrganizationRoleType,
  ) {
    val managedBy = getManagedBy(user.id)
    if (managedBy != null && managedBy.id != organization.id) {
      throw ValidationException(Message.USER_IS_MANAGED_BY_ORGANIZATION)
    }
    OrganizationRole(user = user, organization = organization, type = organizationRoleType)
      .let {
        organization.memberRoles.add(it)
        user.organizationRoles.add(it)
        organizationRoleRepository.save(it)
      }
  }

  fun leave(organizationId: Long) {
    self.removeUser(authenticationFacade.authenticatedUser.id, organizationId)
  }

  @Transactional
  fun removeUser(
    userId: Long,
    organizationId: Long,
  ) {
    if (!canRemoveUser(userId, organizationId)) {
      throw ValidationException(Message.USER_IS_MANAGED_BY_ORGANIZATION)
    }

    removeUserForReal(userId, organizationId)
  }

  @Transactional
  fun removeOrDeactivateUser(
    userId: Long,
    organizationId: Long,
  ) {
    if (!canRemoveUser(userId, organizationId)) {
      userAccountService.disable(userId)
      return
    }

    removeUserForReal(userId, organizationId)
  }

  /**
   * Checks if a user is managed by the organization.
   * We can't remove managed users from their organization.
   */
  private fun canRemoveUser(
    userId: Long,
    organizationId: Long,
  ): Boolean {
    val managedBy = getManagedBy(userId)
    val isManaged = managedBy != null

    if (!isManaged) {
      // Not managed by any organization
      return true
    }

    if (managedBy.id != organizationId) {
      // Managed by another organization
      return true
    }

    // User is managed by the organization - we can't remove them
    return false
  }

  private fun removeUserForReal(
    userId: Long,
    organizationId: Long,
  ) {
    val role =
      organizationRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)?.let {
        organizationRoleRepository.delete(it)
        it
      }
    val permissions = permissionService.removeAllProjectInOrganization(organizationId, userId)

    if (role == null && permissions.isEmpty()) {
      throw NotFoundException(Message.USER_IS_NOT_MEMBER_OF_ORGANIZATION)
    }

    userPreferencesService.refreshPreferredOrganization(userId)
    evictCache(organizationId, userId)
  }

  fun onOrganizationDelete(organization: Organization) {
    organizationRoleRepository.deleteByOrganization(organization)
  }

  fun grantMemberRoleToUser(
    user: UserAccount,
    organization: Organization,
  ) {
    self.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.MEMBER)
  }

  fun grantMaintainerRoleToUser(
    user: UserAccount,
    organization: Organization,
  ) {
    self.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.MAINTAINER)
  }

  fun grantOwnerRoleToUser(
    user: UserAccount,
    organization: Organization,
  ) {
    self.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.OWNER)
  }

  fun setMemberRole(
    organizationId: Long,
    userId: Long,
    dto: SetOrganizationRoleDto,
  ) {
    val user = userAccountService.findActive(userId) ?: throw NotFoundException()
    organizationRoleRepository.findOneByUserIdAndOrganizationId(user.id, organizationId)?.let {
      it.type = dto.roleType
      organizationRoleRepository.save(it)
    } ?: throw ValidationException(Message.USER_IS_NOT_MEMBER_OF_ORGANIZATION)
    evictCache(organizationId, userId)
  }

  fun createForInvitation(
    invitation: Invitation,
    type: OrganizationRoleType,
    organization: Organization,
  ): OrganizationRole {
    return OrganizationRole(invitation = invitation, type = type, organization = organization).let {
      organizationRoleRepository.save(it)
    }
  }

  fun acceptInvitation(
    organizationRole: OrganizationRole,
    userAccount: UserAccount,
  ) {
    organizationRole.invitation = null
    organizationRole.user = userAccount
    organizationRoleRepository.save(organizationRole)
    // switch user to the organization when accepted invitation
    organizationRole.organization?.let {
      userPreferencesService.setPreferredOrganization(it, userAccount)
      evictCache(it.id, userAccount.id)
    }
  }

  fun isAnotherOwnerInOrganization(id: Long): Boolean {
    return this.organizationRoleRepository
      .countAllByOrganizationIdAndTypeAndUserIdNot(
        id,
        OrganizationRoleType.OWNER,
        authenticationFacade.authenticatedUser.id,
      ) > 0
  }

  fun saveAll(organizationRoles: List<OrganizationRole>) {
    organizationRoleRepository.saveAll(organizationRoles)
    organizationRoles.forEach {
      evictForRole(it)
    }
  }

  private fun evictForRole(it: OrganizationRole) {
    val organizationId = it.organization?.id
    val userId = it.user?.id
    if (organizationId != null && userId != null) {
      evictCache(organizationId, userId)
    }
  }

  fun evictCache(
    organizationId: Long,
    userId: Long,
  ) {
    val cache = cacheManager.getCache(Caches.ORGANIZATION_ROLES)
    cache?.evict(arrayListOf(organizationId, userId))
  }

  @Transactional
  fun getOwners(organization: Organization): List<UserAccount> {
    return organizationRoleRepository.getOwners(organization)
  }
}
