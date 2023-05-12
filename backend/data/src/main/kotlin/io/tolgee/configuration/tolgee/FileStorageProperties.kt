/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage")
class FileStorageProperties(
  var s3: S3Settings = S3Settings(),
  @Schema(description = "Path to directory where Tolgee will store its files.", defaultValue = "`~/.tolgee/`, with docker `/data/`")
  var fsDataPath: String = """${System.getProperty("user.home")}/.tolgee""",
)
