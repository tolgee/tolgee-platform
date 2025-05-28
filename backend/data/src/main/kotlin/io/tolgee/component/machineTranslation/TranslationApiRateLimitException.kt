package io.tolgee.component.machineTranslation

class TranslationApiRateLimitException(val retryAt: Long, cause: Throwable? = null) :
  RuntimeException("Translation API rate limit exceeded", cause)
