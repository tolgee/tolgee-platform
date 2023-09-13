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

package io.tolgee.security.ratelimit

/**
 * Enables rate limiting on a specific route in addition to the global rate limit.
 * Has no effect when rate limiting is disabled in server configuration.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class RateLimited(
  /**
   * Maximum amount of requests that can be made during the window interval.
   */
  val limit: Int,
  /**
   * Time it will take for the bucket to refill, in milliseconds. Defaults to a second.
   */
  val refillDurationInMs: Long = 1_000,
  /**
   * Allows for grouping different endpoints under the same bucket. Will default to "{method} {path}".
   */
  val bucketName: String = "",
  /**
   * Number of path variables to take into account during bucketing.
   *
   * For instance, GET `/v2/projects/1/activity` and `/v2/projects/2/activity` will be considered different buckets
   * despite the route being the same (`/v2/projects/{id}/activity`).
   */
  val pathVariablesToDiscriminate: Int = 1,
  /**
   * Whether this rate limit applies to an authentication-related endpoint.
   *
   * This controls the config flag used to determine if the limit should be applied or not.
   */
  val isAuthentication: Boolean = false,
)
