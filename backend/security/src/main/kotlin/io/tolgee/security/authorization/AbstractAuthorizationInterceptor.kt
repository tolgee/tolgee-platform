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

package io.tolgee.security.authorization

import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authentication.WriteOperation
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import kotlin.jvm.java

abstract class AbstractAuthorizationInterceptor(
  val allowGlobalRoutes: Boolean = true,
) : HandlerInterceptor, Ordered {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    if (handler !is HandlerMethod || DispatcherType.ASYNC == request.dispatcherType) {
      return super.preHandle(request, response, handler)
    }

    if (request.method == "OPTIONS") {
      // Do not process OPTIONS requests
      return true
    }

    // Global route; abort here
    if (allowGlobalRoutes && isGlobal(handler)) return true

    return preHandleInternal(request, response, handler)
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }

  abstract fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean

  private fun isGlobal(handler: HandlerMethod): Boolean {
    val annotation = AnnotationUtils.getAnnotation(handler.method, IsGlobalRoute::class.java)
    return annotation != null
  }

  /**
   * Determines if the target endpoint is read-only. Can be overridden by annotating the method with
   * [ReadOnlyOperation] or [WriteOperation] annotation.
   *
   * @param usesWritePermissions whether the request uses write permissions; if false, the method is
   * considered read-only; ignored if null
   */
  fun isReadOnlyMethod(
    request: HttpServletRequest,
    handler: HandlerMethod,
    usesWritePermissions: Boolean? = null
  ): Boolean {
    val forceReadOnly = AnnotationUtils.getAnnotation(handler.method, ReadOnlyOperation::class.java) != null
    val forceWrite = AnnotationUtils.getAnnotation(handler.method, WriteOperation::class.java) != null

    if (forceReadOnly && forceWrite) {
      // This doesn't make sense
      throw RuntimeException(
        "Both `@ReadOnlyOperation` and `@WriteOperation` have been set for this endpoint!",
      )
    }

    if (forceWrite) {
      return false
    }

    if (forceReadOnly) {
      return true
    }

    return request.method in READ_ONLY_METHODS || usesWritePermissions == false
  }

  companion object {
    val READ_ONLY_METHODS = arrayOf("GET", "HEAD", "OPTIONS")
  }
}
