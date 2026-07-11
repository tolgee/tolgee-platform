package io.tolgee.fixtures

fun retry(
  retries: Int = 3,
  exceptionMatcher: (Throwable) -> Boolean = { true },
  fn: () -> Unit,
) {
  val thrown = mutableListOf<Throwable>()
  var passed = false
  while (thrown.size < retries + 1) {
    try {
      fn()
      passed = true
      break
    } catch (th: Throwable) {
      if (exceptionMatcher(th)) {
        thrown.add(th)
      } else {
        throw th
      }
      passed = false
    }
  }
  if (passed) {
    return
  }

  throw RetryException(retries, thrown)
}

class RetryException(
  val retryCount: Int,
  val causes: List<Throwable>,
) : Exception(
    "Test failed in $retryCount retries",
    causes.last(),
  )
