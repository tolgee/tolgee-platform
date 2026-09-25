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
 * Which resolver pool and stuck-host memo a document fetch runs on. The lanes share neither: a shared one would let
 * an arriving request decide what the background check may read.
 */
enum class CimdFetchLane {
  /** `/oauth2/authorize` reading the document of a client nobody has consented to yet. */
  REQUEST,

  /** The scheduled check re-reading the document of a client that already holds a grant. */
  GRANT_CHECK,
}
