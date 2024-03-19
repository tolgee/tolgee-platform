package io.tolgee.api

import io.swagger.v3.oas.annotations.media.Schema

interface ISimpleProject {
  val id: Long
  val name: String
  val description: String?
  val slug: String?

  @get:Schema(description = "Whether to disable ICU placeholder visualization in the editor and it's support.")
  val icuPlaceholders: Boolean
  val avatarHash: String?
}
