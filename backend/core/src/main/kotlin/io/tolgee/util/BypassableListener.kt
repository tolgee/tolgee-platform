package io.tolgee.util

interface BypassableListener {
  var bypass: Boolean

  fun <T> bypassingListener(fn: () -> T): T {
    val oldBypass = bypass
    try {
      bypass = true
      val ret = fn()
      return ret
    } finally {
      bypass = oldBypass
    }
  }

  fun <T> executeIfNotBypassed(fn: () -> T): T? {
    if (!bypass) {
      return fn()
    }
    return null
  }
}
