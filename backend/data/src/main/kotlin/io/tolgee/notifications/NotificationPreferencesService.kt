/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.notifications

import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.NotificationPreferences
import io.tolgee.notifications.dto.NotificationPreferencesDto
import io.tolgee.repository.notifications.NotificationPreferencesRepository
import io.tolgee.service.security.SecurityService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class NotificationPreferencesService(
  private val entityManager: EntityManager,
  private val notificationPreferencesRepository: NotificationPreferencesRepository,
  private val securityService: SecurityService,
) {
  fun getAllPreferences(user: Long): Map<String, NotificationPreferencesDto> {
    return notificationPreferencesRepository.findAllByUserAccountId(user)
      .associate {
        val key = it.project?.id?.toString() ?: "global"
        val dto = NotificationPreferencesDto.fromEntity(it)
        Pair(key, dto)
      }
  }

  fun getGlobalPreferences(user: Long): NotificationPreferencesDto {
    val entity =
      notificationPreferencesRepository.findByUserAccountIdAndProjectId(user, null)
        ?: return NotificationPreferencesDto.createBlank()

    return NotificationPreferencesDto.fromEntity(entity)
  }

  fun getProjectPreferences(
    user: Long,
    project: Long,
  ): NotificationPreferencesDto {
    // If the user cannot see the project, the project "does not exist".
    val scopes = securityService.getProjectPermissionScopes(project, user) ?: emptyArray()
    if (scopes.isEmpty()) throw NotFoundException()

    val entity =
      notificationPreferencesRepository.findByUserAccountIdAndProjectId(user, null)
        ?: throw NotFoundException()

    return NotificationPreferencesDto.fromEntity(entity)
  }

  fun setPreferencesOfUser(
    user: Long,
    dto: NotificationPreferencesDto,
  ): NotificationPreferences {
    return setPreferencesOfUser(
      entityManager.getReference(UserAccount::class.java, user),
      dto,
    )
  }

  fun setPreferencesOfUser(
    user: UserAccount,
    dto: NotificationPreferencesDto,
  ): NotificationPreferences {
    return doSetPreferencesOfUser(user, null, dto)
  }

  fun setProjectPreferencesOfUser(
    user: Long,
    project: Long,
    dto: NotificationPreferencesDto,
  ): NotificationPreferences {
    return setProjectPreferencesOfUser(
      entityManager.getReference(UserAccount::class.java, user),
      entityManager.getReference(Project::class.java, project),
      dto,
    )
  }

  fun setProjectPreferencesOfUser(
    user: UserAccount,
    project: Project,
    dto: NotificationPreferencesDto,
  ): NotificationPreferences {
    // If the user cannot see the project, the project "does not exist".
    val scopes = securityService.getProjectPermissionScopes(project.id, user.id) ?: emptyArray()
    if (scopes.isEmpty()) throw NotFoundException()

    return doSetPreferencesOfUser(user, project, dto)
  }

  private fun doSetPreferencesOfUser(
    user: UserAccount,
    project: Project?,
    dto: NotificationPreferencesDto,
  ): NotificationPreferences {
    // Hidden as a private function as the fact global prefs and project overrides are the "same" is an implementation
    // detail that should not be relied on by consumer of this service.
    return notificationPreferencesRepository.save(
      NotificationPreferences(
        user,
        project,
        dto.disabledNotifications.toTypedArray(),
      ),
    )
  }

  fun deleteProjectPreferencesOfUser(
    user: Long,
    project: Long,
  ) {
    notificationPreferencesRepository.deleteByUserAccountIdAndProjectId(user, project)
  }

  fun deleteAllByUserId(userId: Long) {
    notificationPreferencesRepository.deleteAllByUserId(userId)
  }

  fun deleteAllByProjectId(projectId: Long) {
    notificationPreferencesRepository.deleteAllByProjectId(projectId)
  }
}
