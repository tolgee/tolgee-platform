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

package io.tolgee.repository.notifications

import io.tolgee.model.notifications.NotificationPreferences
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NotificationPreferencesRepository : JpaRepository<NotificationPreferences, Long> {
  fun findAllByUserAccountId(user: Long): List<NotificationPreferences>

  fun findByUserAccountIdAndProjectId(
    user: Long,
    project: Long?,
  ): NotificationPreferences?

  fun deleteByUserAccountIdAndProjectId(
    user: Long,
    project: Long,
  )

  @Modifying
  @Query("DELETE FROM NotificationPreferences WHERE userAccount.id = :userId")
  fun deleteAllByUserId(userId: Long)

  @Modifying
  @Query("DELETE FROM NotificationPreferences WHERE project.id = :projectId")
  fun deleteAllByProjectId(projectId: Long)
}
