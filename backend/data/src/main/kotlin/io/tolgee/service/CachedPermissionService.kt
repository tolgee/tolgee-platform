@file:Suppress("SpringElInspection")

package io.tolgee.service

import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Invitation
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.PermissionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    permission.project.permissions.add(permission)
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
  fun createForInvitation(
    invitation: Invitation,
    project: Project,
    type: ProjectPermissionType,
    languages: Collection<Language>?
  ): Permission {
    validateTranslatePermissionLanguages(languages, type)
    return Permission(invitation = invitation, project = project, type = type).let { permission ->
      languages?.let {
        permission.languages = languages.toMutableSet()
      }
      permissionRepository.save(permission)
    }
  }

  private fun validateTranslatePermissionLanguages(
    languages: Collection<Language>?,
    type: ProjectPermissionType
  ) {
    if (!languages.isNullOrEmpty() && type != ProjectPermissionType.TRANSLATE) {
      throw BadRequestException(Message.ONLY_TRANSLATE_PERMISSION_ACCEPTS_LANGUAGES)
    }
  }

  fun findOneByProjectIdAndUserId(projectId: Long, userId: Long): Permission? {
    return permissionRepository.findOneByProjectIdAndUserId(projectId, userId)
  }

  @Cacheable(
    cacheNames = [Caches.PROJECT_PERMISSIONS],
    key = "{#userId, #projectId}",
  )
  @Transactional
  fun findOneDtoByProjectIdAndUserId(projectId: Long, userId: Long): PermissionDto? {
    return permissionRepository.findOneByProjectIdAndUserId(projectId, userId)?.let { permission ->
      PermissionDto(
        userId = permission.user?.id,
        invitationId = permission.invitation?.id,
        id = permission.id,
        type = permission.type,
        projectId = permission.project.id,
        languageIds = permission.languages.map { it.id }.toMutableSet()
      )
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
