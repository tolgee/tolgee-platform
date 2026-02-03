package io.tolgee.util

import org.springframework.http.ResponseEntity

fun ResponseEntity.BodyBuilder.disableAccelBuffering(): ResponseEntity.BodyBuilder {
  this.headers {
    it.add("X-Accel-Buffering", "no")
  }
  return this
}
