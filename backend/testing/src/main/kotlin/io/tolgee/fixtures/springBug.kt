package io.tolgee.fixtures

import org.opentest4j.TestAbortedException

/**
 * Main link to the spring bug
 * https://github.com/spring-projects/spring-security/issues/9175
 * It doesn't seem to be fixed soon. The best explanation of what is going on is here by Rossen Stoyanchev:
 * https://github.com/spring-projects/spring-security/issues/11452#issuecomment-1172491187
 *
 * Happens for async request processing, for example when using StreamingResponseBody.
 *
 * You can see in this method where the ConcurrentModification actually happens ("addHeader" <-> "setHeader").
 * Also for more details check the pull request discussion: https://github.com/tolgee/tolgee-platform/pull/3233
 *
 * TODO(spring upgrade), check if the bug is fixed in spring and remove this ugly workaround
 */
fun <T> ignoreTestOnSpringBug(fn: () -> T): T {
  return try {
    fn.invoke()
  } catch (e: Exception) {
    if (isSpringBug(e)) {
      org.slf4j.LoggerFactory
        .getLogger("springBug")
        .error(e.message, e)
      // Retrying the request once more can still lead to the bug, plus it will hide
      // this dirty "fix" completely. So TestAbortedException is chosen to mark the test
      // as ignored. This way it won't fail the cicd pipeline, and you still see it in the final
      // report, that it was ignored.
      throw TestAbortedException("spring-security/issues/9175", e)
    } else {
      throw e
    }
  }
}

private fun isSpringBug(e: Exception): Boolean {
  return when (e) {
    is ConcurrentModificationException -> {
      return e.stackTrace
        .first()
        .className
        .contains("HashMap") &&
        e.stackTrace.first().methodName == "computeIfAbsent" &&
        e.stackTrace.any {
          it.className.contains("HttpServletResponseWrapper") &&
            (it.methodName == "addHeader" || it.methodName == "setHeader")
        }
    }

    is IllegalStateException -> {
      // this one is thrown by mockMvc. Somewhere inside backend code, if you wrap it in try/catch,
      // you will find the ConcurrentModificationException (e.g. MtResultStreamer when writer is flushed)
      return e.stackTrace.any {
        it.className.contains("HeaderValueHolder") &&
          it.methodName == "getStringValues"
      }
    }

    else -> false
  }
}
