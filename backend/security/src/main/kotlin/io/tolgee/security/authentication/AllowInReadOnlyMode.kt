/**
 * Copyright (C) 2025 Tolgee s.r.o. and contributors
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
 * Overrides read-only request method restriction for the annotated controller or handler method.
 *
 * When current authentication has readOnly flag set, only GET/HEAD/OPTIONS HTTP methods are allowed by default.
 * Applying this annotation to a controller class or method allows non-safe methods as well.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowInReadOnlyMode
