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

import io.tolgee.model.Pat
import java.io.Serializable
import java.util.Date

data class PatDto(
  val id: Long,
  val hash: String,
  val expiresAt: Date?,
  val userAccountId: Long,
) : Serializable {
  companion object {
    fun fromEntity(pat: Pat): PatDto {
      return PatDto(
        id = pat.id,
        hash = pat.tokenHash,
        expiresAt = pat.expiresAt,
        userAccountId = pat.userAccount.id,
      )
    }
  }
}
