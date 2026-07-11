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

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.security.authentication.AuthenticationFacade
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import java.time.Duration

@Component
class RateLimitInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  private val rateLimitService: RateLimitService,
) : HandlerInterceptor,
  Ordered {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    if (handler !is HandlerMethod || DispatcherType.ASYNC == request.dispatcherType) {
      return super.preHandle(request, response, handler)
    }

    val policy = extractEndpointRateLimit(request, authenticationFacade.authenticatedUserOrNull, handler)
    if (policy != null) {
      rateLimitService.consumeBucket(policy)
    }

    return true
  }

  fun extractEndpointRateLimit(
    request: HttpServletRequest,
    account: UserAccountDto?,
    handler: HandlerMethod,
  ): RateLimitPolicy? {
    val annotation =
      AnnotationUtils.getAnnotation(handler.method, RateLimited::class.java)
        ?: return null

    if (!rateLimitService.shouldRateLimit(annotation.isAuthentication)) {
      return null
    }

    val bucketName =
      getBucketName(request, annotation, account)

    return RateLimitPolicy(
      bucketName,
      annotation.limit,
      Duration.ofMillis(annotation.refillDurationInMs),
      false,
    )
  }

  private fun getBucketName(
    request: HttpServletRequest,
    annotation: RateLimited,
    account: UserAccountDto?,
  ): String {
    val matchedPath = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
    val pathVariablesMap = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>

    val id = if (account?.id != null) "user.${account.id}" else "ip.${request.remoteAddr}"
    val pathVariables = pathVariablesMap.values.iterator()

    var bucketName = "endpoint.$id.${annotation.bucketName.ifEmpty { "${request.method} $matchedPath" }}"

    // Include path variables to discriminate major routes, but not minor routes.
    // Example: These are different
    //  - /major/1/some-path -> "/major/{id}/some-path 1"
    //  - /major/2/some-path -> "/major/{id}/some-path 2"
    // However, for minor routes: These are the same
    //  - /major/1/minor/1/some-path -> "/major/{id}/minor/{subId}/some-path 1"
    //  - /major/1/minor/2/some-path -> "/major/{id}/minor/{subId}/some-path 1"

    var i = 0
    while (i < annotation.pathVariablesToDiscriminate && pathVariables.hasNext()) {
      bucketName += pathVariables.next()
      i++
    }

    return bucketName
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }
}
