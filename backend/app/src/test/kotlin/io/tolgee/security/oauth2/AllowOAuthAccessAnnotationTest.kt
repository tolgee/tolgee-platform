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

package io.tolgee.security.oauth2

import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AllowOAuthAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * `@AllowOAuthAccess` only opens an endpoint when its `@AllowApiAccess` is `tokenType = ANY`; in any other
 * combination `AuthenticationInterceptor` refuses the request, so the annotation would read as if it worked while
 * doing nothing.
 */
class AllowOAuthAccessAnnotationTest : AbstractControllerTest() {
  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private lateinit var handlerMapping: RequestMappingHandlerMapping

  @Test
  fun `every endpoint opened to OAuth also admits OAuth tokens`() {
    val opened =
      handlerMapping.handlerMethods.values
        .filter { AnnotationUtils.getAnnotation(it.method, AllowOAuthAccess::class.java) != null }

    // Without this the whole test passes by matching nothing: moving the annotation to class level, where
    // AnnotationUtils.getAnnotation(method, ...) cannot see it, would silently retire the invariant.
    opened.assert
      .withFailMessage("no endpoint carries @AllowOAuthAccess, so this test is checking nothing")
      .isNotEmpty()

    val offenders =
      opened
        .filter { AnnotationUtils.getAnnotation(it.method, AllowApiAccess::class.java)?.tokenType != AuthTokenType.ANY }
        .map { "${it.beanType.simpleName}.${it.method.name}" }

    offenders.assert
      .withFailMessage(
        "annotated @AllowOAuthAccess but their @AllowApiAccess is missing or not tokenType = ANY, so the " +
          "annotation opens nothing: %s",
        offenders,
      ).isEmpty()
  }
}
