/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
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

package io.tolgee.security.oauth2.cimd

/**
 * We had no capacity to even try: every resolver thread was stuck on a lookup of its own. Thrown rather than
 * returned as a [CimdResolution], so it passes through Caffeine's loader without a value being stored.
 */
class CimdNoCapacityException(
  clientIdUrl: String,
) : RuntimeException("no capacity to resolve $clientIdUrl")
