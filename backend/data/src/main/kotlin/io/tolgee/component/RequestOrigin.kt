package io.tolgee.component

import org.springframework.web.servlet.support.ServletUriComponentsBuilder

internal fun currentRequestOriginOrNull(): String? {
  try {
    val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
    builder.replacePath("")
    builder.replaceQuery("")
    return builder.build().toUriString()
  } catch (e: IllegalStateException) {
    if (e.message?.contains("No current ServletRequestAttributes") == true) {
      return null
    }
    throw e
  }
}
