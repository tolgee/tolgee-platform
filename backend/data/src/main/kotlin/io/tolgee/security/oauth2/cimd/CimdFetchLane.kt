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
 * Which lane a document fetch runs on. The two lanes share nothing that one could leave behind for the other: not
 * the resolver pool, not the stuck-host memo, and not the cache.
 *
 * In one direction, anything the request lane may fill is something an anonymous caller fills on purpose: a shared
 * pool, memo or cache would let an arriving request decide what the check may read for a client somebody holds a
 * grant for. In the other, what the check reads must never warm the lane `/oauth2/authorize` answers from, because
 * the check runs only for clients somebody on this instance consented to, so a warm entry or a faster answer would
 * tell a caller polling the endpoint that someone here uses that app.
 */
enum class CimdFetchLane {
  /** `/oauth2/authorize` reading the document of a client nobody has consented to yet. */
  REQUEST,

  /** The document check on the refresh path re-reading the document of a client that already holds a grant. */
  GRANT_CHECK,
}
