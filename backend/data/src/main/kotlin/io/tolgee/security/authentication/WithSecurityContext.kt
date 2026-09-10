package io.tolgee.security.authentication

import org.springframework.security.core.context.SecurityContextHolder

/**
 * Runs [body] with [authentication] in the security context.
 */
fun <T> withSecurityContext(
  authentication: TolgeeAuthentication,
  body: () -> T,
): T {
  val previous = SecurityContextHolder.getContext()
  try {
    SecurityContextHolder.setContext(
      SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication },
    )
    return body()
  } finally {
    SecurityContextHolder.setContext(previous)
  }
}
