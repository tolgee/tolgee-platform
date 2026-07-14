package io.tolgee.api

import io.swagger.v3.oas.annotations.media.Schema

interface IKeyModel {
  @get:Schema(description = "Id of key record")
  val id: Long

  @get:Schema(description = "Name of key", example = "this_is_super_key")
  val name: String

  @get:Schema(description = "Namespace of key", example = "homepage")
  val namespace: String?

  @get:Schema(
    description = "Description of key",
    example = "This key is used on homepage. It's a label of sign up button.",
  )
  val description: String?

  @get:Schema(description = "Custom values of the key")
  val custom: Map<String, Any?>?
}
