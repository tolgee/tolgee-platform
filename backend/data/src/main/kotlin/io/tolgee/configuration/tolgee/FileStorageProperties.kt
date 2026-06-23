/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.NestedConfigurationProperty

@DocProperty(
  prefix = "tolgee.file-storage",
  description = "Configuration of Tolgee file storage.",
  displayName = "File storage",
)
class FileStorageProperties(
  @NestedConfigurationProperty
  var s3: S3Settings = S3Settings(),
  @DocProperty(
    description = "Path to directory where Tolgee will store its files.",
    defaultExplanation = ", with docker `/data/`",
    defaultValue = "~/.tolgee/",
  )
  var fsDataPath: String = """${System.getProperty("user.home")}/.tolgee""",
)
