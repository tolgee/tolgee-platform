package io.tolgee.component.automations.processors

import org.springframework.http.HttpStatusCode

class WebhookRespondedWithNon200Status(
  statusCode: HttpStatusCode,
  body: Any?,
) : WebhookException()

class WebhookExecutionFailed(
  e: Throwable,
) : WebhookException(e)

open class WebhookException(
  cause: Throwable? = null,
) : RuntimeException("Webhook execution failed", cause)
