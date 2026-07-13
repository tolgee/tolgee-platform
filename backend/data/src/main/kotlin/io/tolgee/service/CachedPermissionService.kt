@file:Suppress("SpringElInspection")

package io.tolgee.service

import io.tolgee.constants.Caches
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.repository.PermissionRepository
import io.tolgee.service.project.ProjectService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CachedPermissionService(
  private val permissionRepository: PermissionRepository,
) {
  @set:Autowired
  lateinit var projectService: ProjectService

  fun find(id: Long): Permission? {
    return permissionRepository.findById(id).orElse(null)
  }

  fun create(permission: Permission): Permission {
    return permissionRepository.save(permission)
  }

  @CacheEvict(
    cacheNames = [Caches.PERMISSIONS],
    key = "{#permission.user?.id, #permission.project?.id, #permission.organization?.id}",
  )
  fun delete(permission: Permission) {
    permissionRepository.delete(permission)
  }

  @Cacheable(
    cacheNames = [Caches.PERMISSIONS],
    key = "{#userId, #projectId, #organizationId}",
  )
  @Transactional
  fun find(
    projectId: Long? = null,
    userId: Long? = null,
    organizationId: Long? = null,
  ): PermissionDto? {
    return permissionRepository
      .findOneByProjectIdAndUserIdAndOrganizationId(
        projectId = projectId,
        userId = userId,
        organizationId = organizationId,
      )?.let { permission ->
        PermissionDto(
          id = permission.id,
          userId = permission.user?.id,
          invitationId = permission.invitation?.id,
          scopes = permission.scopes,
          projectId = permission.project?.id,
          organizationId = permission.organization?.id,
          translateLanguageIds = permission.translateLanguageIds,
          viewLanguageIds = permission.viewLanguageIds,
          stateChangeLanguageIds = permission.stateChangeLanguageIds,
          suggestLanguageIds = permission.suggestLanguageIds,
          type = permission.type,
          granular = permission.granular,
        )
      }
  }

  @CacheEvict(
    cacheNames = [Caches.PERMISSIONS],
    key = "{#permission.user?.id, #permission.project?.id, #permission.organization?.id}",
  )
  fun acceptInvitation(
    permission: Permission,
    userAccount: UserAccount,
  ): Permission {
    permission.invitation = null
    permission.user = userAccount
    return permissionRepository.save(permission)
  }

  @CacheEvict(
    cacheNames = [Caches.PERMISSIONS],
    key = "{#result.user?.id, #result.project?.id, #result.organization?.id}",
  )
  fun save(permission: Permission): Permission {
    return permissionRepository.save(permission)
  }
}
