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

import com.fasterxml.jackson.databind.ObjectMapper
import io.sentry.Sentry
import io.tolgee.exceptions.ErrorException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.ExpectedException
import io.tolgee.exceptions.NotFoundException
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.io.OutputStreamWriter

@Component
class StreamingResponseBodyProvider(
  private val entityManager: EntityManager,
  private val objectMapper: ObjectMapper,
) : Logging {
  fun createStreamingResponseBody(fn: (os: OutputStream) -> Unit): StreamingResponseBody {
    return StreamingResponseBody {
      val session = entityManager.unwrap(Session::class.java)

      session.doWork { connection ->
        fn(it)
        // Manually dispose the connection because spring has a hard time doing so by itself
        connection.close()
      }

      // Manually dispose the connection because spring has a hard time doing so by itself
      session.close()
    }
  }

  fun streamNdJson(stream: (write: (message: Any?) -> Unit) -> Unit): ResponseEntity<StreamingResponseBody> {
    return ResponseEntity.ok().disableAccelBuffering().body(
      this.createStreamingResponseBody { outputStream ->
        OutputStreamWriter(outputStream).use { writer ->
          val write =
            { message: Any? -> writer.writeJson(message) }
          try {
            stream(write)
          } catch (e: Throwable) {
            val message = getErrorMessage(e)
            writer.writeJson(StreamedErrorMessage(message))
            logger.debug("Error while streaming response body", e)
            if (e !is ExpectedException) {
              Sentry.captureException(e)
            }
          }
        }
      },
    )
  }

  fun OutputStreamWriter.writeJson(message: Any?) {
    this.write(
      (objectMapper.writeValueAsString(message) + "\n"),
    )
    this.flush()
  }

  private fun getErrorMessage(e: Throwable) =
    when (e) {
      is NotFoundException -> ErrorResponseBody(e.msg.code, null)
      is ErrorException -> e.errorResponseBody
      else ->
        ErrorResponseBody(
          "unexpected_error_occurred",
          listOf(e::class.java.name),
        )
    }

  data class StreamedErrorMessage(
    val error: ErrorResponseBody,
  )
}
