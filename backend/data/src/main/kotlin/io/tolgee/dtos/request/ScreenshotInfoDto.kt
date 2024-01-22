package io.tolgee.dtos.request

class ScreenshotInfoDto(
  var text: String? = null,
  var positions: List<KeyInScreenshotPositionDto>? = null,
  var location: String? = null,
)
