package io.tolgee.dtos.request

data class GetTranslationsParamsDto(
        val languages: Set<String>? = null,
        val search: String? = null,
        val keyName: String? = null,
        val keyId: Long? = null
)
