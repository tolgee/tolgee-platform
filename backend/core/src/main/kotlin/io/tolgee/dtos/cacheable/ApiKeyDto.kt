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

package io.tolgee.dtos.cacheable

import io.tolgee.model.ApiKey
import io.tolgee.model.enums.Scope
import java.io.Serializable
import java.util.Date

data class ApiKeyDto(
  val id: Long,
  val hash: String,
  val expiresAt: Date?,
  val projectId: Long,
  val userAccountId: Long,
  val scopes: Set<Scope>,
) : Serializable {
  companion object {
    fun fromEntity(apiKey: ApiKey): ApiKeyDto {
      return ApiKeyDto(
        id = apiKey.id,
        hash = apiKey.keyHash,
        expiresAt = apiKey.expiresAt,
        projectId = apiKey.project.id,
        userAccountId = apiKey.userAccount.id,
        scopes = apiKey.scopesEnum.filterNotNull().toSet(),
      )
    }
  }
}
