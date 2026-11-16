package io.tolgee.dtos.request.notification

import io.swagger.v3.oas.annotations.media.Schema

class NotificationLanguagesRequest(
  @Schema(
    description =
      "Languages to follow for work notifications. Empty mutes the project, null goes back to the default " +
        "picked from recent work.",
    example = "[1000000001]",
  )
  var languageIds: List<Long>? = null,
)
