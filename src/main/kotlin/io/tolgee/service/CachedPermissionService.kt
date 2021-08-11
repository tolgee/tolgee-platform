@file:Suppress("SpringElInspection")

package io.tolgee.service

import io.tolgee.configuration.Caches
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.PermissionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CachedPermissionService(
  private val permissionRepository: PermissionRepository
) {
  @set:Autowired
  lateinit var projectService: ProjectService

  fun getAllOfProject(project: Project?): Set<Permission> {
    return permissionRepository.getAllByProjectAndUserNotNull(project)
  }

  fun findById(id: Long): Permission? {
    return permissionRepository.findById(id).orElse(null)
  }

  fun create(permission: Permission): Permission {
    permission.project!!.permissions.add(permission)
    return permissionRepository.save(permission)
  }

  @CacheEvict(
    cacheNames = [Caches.PROJECT_PERMISSIONS],
    key = "{#permission.user?.id, #permission.project?.id}"
  )
  fun delete(permission: Permission) {
    permissionRepository.delete(permission)
  }

  @CacheEvict(
    cacheNames = [Caches.PROJECT_PERMISSIONS],
    key = "{#result.user?.id, #result.project?.id}"
  )
  fun createForInvitation(invitation: Invitation, project: Project, type: ProjectPermissionType): Permission {
    return Permission(invitation = invitation, project = project, type = type).let {
      permissionRepository.save(it)
    }
  }

  fun findOneByProjectIdAndUserId(projectId: Long, userId: Long): Permission? {
    return permissionRepository.findOneByProjectIdAndUserId(projectId, userId)
  }

  @Cacheable(
    cacheNames = [Caches.PROJECT_PERMISSIONS],
    key = "{#userId, #projectId}",
  )
  fun findOneDtoByProjectIdAndUserId(projectId: Long, userId: Long): PermissionDto? {
    return permissionRepository.findOneByProjectIdAndUserId(projectId, userId)?.let {
      return PermissionDto.fromEntity(it)
    }
  }

  @CacheEvict(
    cacheNames = [Caches.PROJECT_PERMISSIONS],
    key = "{#permission.user?.id, #permission.project?.id}"
  )
  fun acceptInvitation(permission: Permission, userAccount: UserAccount): Permission {
    permission.invitation = null
    permission.user = userAccount
    return permissionRepository.save(permission)
  }

  @CacheEvict(
    cacheNames = [Caches.PROJECT_PERMISSIONS],
    key = "{#result.user?.id, #result.project?.id}"
  )
  fun save(permission: Permission): Permission {
    return permissionRepository.save(permission)
  }

  @CacheEvict(
    cacheNames = [Caches.PROJECT_PERMISSIONS],
    allEntries = true
  )
  fun saveAll(permissions: Iterable<Permission>) {
    this.permissionRepository.saveAll(permissions)
  }
}
