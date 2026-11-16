package io.tolgee.hateoas.notification

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.service.notification.FollowedLanguagesState
import java.io.Serializable

data class ProjectNotificationLanguagesModel(
  val projectId: Long,
  val projectName: String,
  @Schema(description = "Non-base languages the user can edit or review in this project")
  val availableLanguages: List<NotificationLanguageModel>,
  val followedLanguageIds: List<Long>,
  @Schema(
    description =
      "DEFAULT: picked from recent work until the user chooses, CHOSEN: the user's choice, " +
        "MUTED: the user chose no languages",
  )
  val state: FollowedLanguagesState,
) : Serializable
