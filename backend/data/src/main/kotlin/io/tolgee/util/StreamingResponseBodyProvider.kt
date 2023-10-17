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

package io.tolgee.util

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

@Component
class StreamingResponseBodyProvider(
  private val entityManager: EntityManager
) {
  fun createStreamingResponseBody(fn: (os: OutputStream) -> Unit): StreamingResponseBody {
    return StreamingResponseBody {
      val session = entityManager.unwrap(Session::class.java)
      fn(it)

      // Manually dispose the connection because spring has a hard time doing so by itself
      session.close()
    }
  }
}
