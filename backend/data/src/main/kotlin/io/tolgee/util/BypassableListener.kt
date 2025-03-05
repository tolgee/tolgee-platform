package io.tolgee.util

interface BypassableListener {
  var bypass: Boolean

  fun <T> bypassingListener(fn: () -> T): T {
    val oldBypass = bypass
    bypass = true
    val ret = fn()
    bypass = oldBypass
    return ret
  }

  fun <T> executeIfNotBypassed(fn: () -> T) {
    if (!bypass) {
      fn()
    }
  }
}
