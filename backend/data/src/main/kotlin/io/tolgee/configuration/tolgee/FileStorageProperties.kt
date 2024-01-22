/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage")
@DocProperty(description = "Configuration of Tolgee file storage.", displayName = "File storage")
class FileStorageProperties(
  var s3: S3Settings = S3Settings(),
  @DocProperty(
    description = "Path to directory where Tolgee will store its files.",
    defaultExplanation = ", with docker `/data/`",
    defaultValue = "~/.tolgee/",
  )
  var fsDataPath: String = """${System.getProperty("user.home")}/.tolgee""",
)
