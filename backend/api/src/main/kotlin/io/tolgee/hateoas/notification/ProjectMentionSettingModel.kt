package io.tolgee.hateoas.notification

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

data class ProjectMentionSettingModel(
  val projectId: Long,
  val projectName: String,
  @Schema(description = "Mentions from this project do not notify the user")
  val muted: Boolean,
) : Serializable
