package io.tolgee.hateoas.translations

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.model.views.KeyWithTranslationsView
import org.springframework.data.domain.Page
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponents

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class KeysWithTranslationsPagedResourcesAssembler(
  val keyWithTranslationsModelAssembler: KeyWithTranslationsModelAssembler,
  val languageModelAssembler: LanguageModelAssembler,
  resolver: HateoasPageableHandlerMethodArgumentResolver? = null,
  baseUri: UriComponents? = null,
) : PagedResourcesAssembler<KeyWithTranslationsView>(resolver, baseUri) {
  fun toTranslationModel(
    entities: Page<KeyWithTranslationsView>,
    selectedLanguages: Collection<LanguageDto>,
    nextCursor: String?,
  ): KeysWithTranslationsPageModel {
    val pageModel = toModel(entities, keyWithTranslationsModelAssembler)
    return KeysWithTranslationsPageModel(
      pageModel,
      selectedLanguages =
        selectedLanguages.map {
          languageModelAssembler.toModel(it)
        },
      nextCursor = nextCursor,
    )
  }
}
