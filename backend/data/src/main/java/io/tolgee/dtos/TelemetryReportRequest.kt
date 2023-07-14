package io.tolgee.dtos

import javax.validation.constraints.NotBlank

class TelemetryReportRequest {
  @NotBlank
  var instanceId: String = ""
  var projectsCount: Long = 0
  var translationsCount: Long = 0
  var languagesCount: Long = 0
  var distinctLanguagesCount: Long = 0
  var usersCount: Long = 0
}
