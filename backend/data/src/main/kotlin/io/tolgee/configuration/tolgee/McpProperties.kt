package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "tolgee.mcp")
@DocProperty(description = "Configuration of Tolgee MCP server.", displayName = "MCP")
@Validated
class McpProperties(
  @field:Positive
  @DocProperty(
    description = "Lifetime of MCP image-upload URLs, in milliseconds.",
    defaultExplanation = "= 30 minutes",
  )
  var imageUploadUrlExpirationMs: Long = 30 * 60 * 1000L,
)
