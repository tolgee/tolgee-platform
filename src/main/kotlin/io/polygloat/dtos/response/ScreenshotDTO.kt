/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.dtos.response

import java.util.*

data class ScreenshotDTO(
        val id: Long,
        val filename: String,
        val createdAt: Date
)