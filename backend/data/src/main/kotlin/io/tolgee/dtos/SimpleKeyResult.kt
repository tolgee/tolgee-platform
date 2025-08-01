package io.tolgee.dtos

import java.io.Serializable

class SimpleKeyResult(
  val id: Long,
  val name: String,
  val namespace: String?,
) : Serializable
