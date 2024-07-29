package io.tolgee.api.v2.controllers.dataImport

import io.sentry.Sentry
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.service.dataImport.status.ImportApplicationStatus
import io.tolgee.service.dataImport.status.ImportApplicationStatusItem
import io.tolgee.util.Logging
import io.tolgee.util.StreamingResponseBodyProvider
import io.tolgee.util.logger
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Component
class StreamingImportProgressUtil(
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
) : Logging {
  fun stream(
    fn: (writeStatus: (status: ImportApplicationStatus) -> Unit) -> Unit,
  ): ResponseEntity<StreamingResponseBody> {
    return streamingResponseBodyProvider.streamNdJson { write ->
      val writeStatus = { status: ImportApplicationStatus ->
        write(ImportApplicationStatusItem(status))
      }
      try {
        fn(writeStatus)
        write(
          ImportApplicationStatusItem(
            ImportApplicationStatus.DONE,
          ),
        )
      } catch (e: Exception) {
        if (e !is BadRequestException) {
          Sentry.captureException(e)
          logger.error("Unexpected error while importing", e)
        }
        when (e) {
          is ErrorException ->
            write(
              ImportApplicationStatusItem(
                ImportApplicationStatus.ERROR,
                errorStatusCode = e.httpStatus.value(),
                errorResponseBody = ErrorResponseBody(e.code, e.params),
              ),
            )

          else ->
            write(
              ImportApplicationStatusItem(
                ImportApplicationStatus.ERROR,
                errorStatusCode = 500,
              ),
            )
        }
      }
    }
  }
}
