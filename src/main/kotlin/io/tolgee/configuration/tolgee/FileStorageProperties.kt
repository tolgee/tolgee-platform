/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage")
class FileStorageProperties(
  var s3: S3Settings = S3Settings(),
  var fsDataPath: String = """${System.getProperty("user.home")}/.tolgee""",
)
