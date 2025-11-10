package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse

@ApiResponse(
  responseCode = "200",
  description =
    "When multiple files are exported, they are zipped and returned as a single zip file." +
      "\n" +
      "When a single file is exported, it is returned directly.",
  content = [Content(mediaType = "application/*")],
)
annotation class ExportApiResponse
