package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.language-tool")
@DocProperty(
  description =
    "Configuration for an external LanguageTool server used for spelling and grammar checks.\n" +
      "LanguageTool container (erikvl87/languagetool:6.7) must be deployed as a separate container.",
  displayName = "LanguageTool",
)
class LanguageToolProperties(
  @DocProperty(
    description =
      "URL of the LanguageTool server (e.g. `http://languagetool:8010`). " +
        "When empty, spelling and grammar checks are disabled.",
  )
  var url: String = "",
  @DocProperty(
    description =
      "Maximum number of concurrent `/v2/check` requests a single Tolgee instance is " +
        "allowed to send to the LanguageTool server.",
  )
  var maxConcurrentRequests: Int = 2,
  @DocProperty(
    description =
      "TCP connect timeout for LanguageTool HTTP calls, in seconds.",
  )
  var connectTimeoutSeconds: Long = 5,
  @DocProperty(
    description =
      "Read (socket) timeout for LanguageTool HTTP calls, in seconds.",
  )
  var readTimeoutSeconds: Long = 60,
)
