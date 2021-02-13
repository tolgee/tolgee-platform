package io.tolgee.dtos.request

import io.tolgee.model.Language
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class LanguageDTO(
        var id: Long? = null,

        @field:NotBlank @field:Size(max = 100)
        var name:  String? = null,

        @field:NotBlank @field:Size(max = 20) @field:Pattern(regexp = "^[^,]*$", message = "can not contain coma")
        var abbreviation: String? = null
) {
    companion object {
        fun fromEntity(language: Language): LanguageDTO {
            val languageDTO = LanguageDTO(id = language.id, name = language.name, abbreviation = language.abbreviation)
            languageDTO.id = language.id
            return languageDTO
        }
    }
}