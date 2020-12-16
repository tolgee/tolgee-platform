package io.polygloat.dtos.response

import io.polygloat.model.Screenshot
import java.util.*

data class ScreenshotDTO(
        val id: Long,
        val filename: String,
        val createdAt: Date
){
    companion object{
        fun fromEntity(entity: Screenshot): ScreenshotDTO {
            return ScreenshotDTO(id = entity.id!!, filename = entity.filename, createdAt = entity.createdAt)
        }
    }
}