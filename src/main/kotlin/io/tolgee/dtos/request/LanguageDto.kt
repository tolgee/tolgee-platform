package io.tolgee.dtos.request

import io.tolgee.model.Language
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class LanguageDto(
        var id: Long? = null,

        @field:NotBlank @field:Size(max = 100)
        var name:  String? = null,

        @field:NotBlank @field:Size(max = 100)
        var originalName: String? = null,

        @field:NotBlank @field:Size(max = 20) @field:Pattern(regexp = "^[^,]*$", message = "can not contain coma")
        var tag: String? = null
) {
    companion object {
        fun fromEntity(language: Language): LanguageDto {
            val languageDTO = LanguageDto(id = language.id, name = language.name, tag = language.tag)
            languageDTO.id = language.id
            return languageDTO
        }
    }
}
