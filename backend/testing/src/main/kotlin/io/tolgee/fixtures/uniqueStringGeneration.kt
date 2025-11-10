package io.tolgee.fixtures

import java.util.UUID

fun generateUniqueString(): String {
  return UUID.randomUUID().toString()
}
