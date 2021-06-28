package io.tolgee.model.views

import io.tolgee.model.enums.TranslationState

data class TranslationView(
        val id: Long,
        val text: String?,
        val state: TranslationState
)
