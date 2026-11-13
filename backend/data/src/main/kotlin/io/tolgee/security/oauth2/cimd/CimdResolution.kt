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
 * What came back when a `client_id` URL was resolved. Only [Withdrawn] is the publisher retiring the client, and
 * only it is worth writing down against the grants. [Rejected] never is, because a broken deploy looks the same.
 */
sealed interface CimdResolution {
  data class Resolved(
    val client: CimdClient,
  ) : CimdResolution

  /** The document is gone: the host answered, and answered that there is nothing there. */
  data object Withdrawn : CimdResolution

  /** Served, but not usable as a client: not served as JSON, or a document that does not validate. */
  data object Rejected : CimdResolution

  data object Unavailable : CimdResolution
}

/** The raw body of a `client_id` document, or why there is none. */
sealed interface CimdDocument {
  data class Body(
    val content: String,
  ) : CimdDocument

  /** 404 or 410: the host is up and says the document does not exist. */
  data object Gone : CimdDocument

  data object Rejected : CimdDocument

  data object Unavailable : CimdDocument
}
