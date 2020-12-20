/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.configuration.polygloat

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polygloat.file-storage")
class FileStorageProperties(
        var s3: S3Settings = S3Settings(),
        var fsDataPath: String = """${System.getProperty("user.home")}/.polygloat""",
)
