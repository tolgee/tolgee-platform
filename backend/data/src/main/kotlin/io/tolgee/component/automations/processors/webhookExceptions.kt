package io.tolgee.component.automations.processors

import io.tolgee.exceptions.ExpectedUserError
import org.springframework.http.HttpStatusCode

class WebhookRespondedWithNon200Status(
  statusCode: HttpStatusCode,
  body: Any?,
) : WebhookException("Webhook responded with non-2xx status $statusCode, body: $body")

class WebhookExecutionFailed(
  e: Throwable,
) : WebhookException("Webhook execution failed: ${e.javaClass.simpleName}: ${e.message}", e)

open class WebhookException(
  message: String = "Webhook execution failed",
  cause: Throwable? = null,
) : RuntimeException(message, cause),
  ExpectedUserError
