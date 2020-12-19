/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.configuration.polygloat

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polygloat.file-storage.s3")
class S3Settings(
        var enabled: Boolean = false,
        var accessKey: String? = null,
        var secretKey: String? = null,
        var endpoint: String? = null,
        var signingRegion: String? = null,
        var bucketName: String? = null,
)
