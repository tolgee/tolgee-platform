package io.tolgee.model.views

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.Translation

data class TranslationView(
        @Schema(description = "Id of translation record")
        val id: Long,

        @Schema(description = "Translation text")
        val text: String?) {

    fun fromEntity(entity: Translation) = TranslationView(id = entity.id, text = entity.text)
}
