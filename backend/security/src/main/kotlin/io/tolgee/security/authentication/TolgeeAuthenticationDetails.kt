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

package io.tolgee.security.authentication

data class TolgeeAuthenticationDetails(
  /**
   * Whether the credentials are a valid Super JWT Token
   */
  val isSuperToken: Boolean,
  /**
   * Whether the credentials used are an API Key (PAK or PAT)
   */
  val isApiKey: Boolean,
  /**
   * ID of the project the credentials are limited to. Only applicable for PAK tokens.
   */
  val projectIdScope: Long?
)
