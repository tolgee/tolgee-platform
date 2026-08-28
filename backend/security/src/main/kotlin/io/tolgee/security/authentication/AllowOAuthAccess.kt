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

package io.tolgee.security.authentication

/**
 * Opens an endpoint to OAuth2 access tokens that [io.tolgee.security.authorization.ProjectScopedEndpoints] would
 * refuse.
 *
 * Put this on an endpoint only once it narrows the token itself: it must read
 * `AuthenticationFacade.oauthTokenCredentials` and honour both the scope set and `coversProject`. Adding it to an
 * endpoint that does neither hands every consented token the user's whole account.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AllowOAuthAccess
