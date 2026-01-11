package io.tolgee.dtos.response

import io.tolgee.dtos.queryResults.KeyWithTranslationsDto

data class KeyWithTranslationsResponseDto(
  var id: Long? = null,
  var name: String? = null,
  var translations: Map<String, String?> = LinkedHashMap(),
) {
  companion object {
    @JvmStatic
    fun fromQueryResult(keyWithTranslationsDTO: KeyWithTranslationsDto): KeyWithTranslationsResponseDto {
      return KeyWithTranslationsResponseDto(
        keyWithTranslationsDTO.id,
        keyWithTranslationsDTO.path.fullPathString,
        keyWithTranslationsDTO.getTranslations(),
      )
    }
  }
}
