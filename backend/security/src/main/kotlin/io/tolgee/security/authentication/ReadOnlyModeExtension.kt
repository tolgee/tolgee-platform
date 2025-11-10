package io.tolgee.security.authentication

import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.method.HandlerMethod

val READ_ONLY_METHODS = arrayOf("GET", "HEAD", "OPTIONS")

/**
 * Determines if the target endpoint is read-only. Can be overridden by annotating the method with
 * [ReadOnlyOperation] or [WriteOperation] annotation.
 */
fun HandlerMethod.isReadOnly(httpMethod: String): Boolean {
  val forceReadOnly = AnnotationUtils.getAnnotation(method, ReadOnlyOperation::class.java) != null
  val forceWrite = AnnotationUtils.getAnnotation(method, WriteOperation::class.java) != null

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

  return httpMethod.uppercase() in READ_ONLY_METHODS
}
