package io.tolgee.hateoas.translations

import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.model.Language
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.LanguageViewImpl
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
    selectedLanguages: Collection<Language>,
    nextCursor: String?,
    baseLanguage: Language?
  ): KeysWithTranslationsPageModel {
    val pageModel = toModel(entities, keyWithTranslationsModelAssembler)
    return KeysWithTranslationsPageModel(
      content = pageModel.content,
      metadata = pageModel.metadata,
      links = pageModel.links.toList().toTypedArray(),
      selectedLanguages = selectedLanguages.map {
        languageModelAssembler.toModel(
          LanguageViewImpl(
            it,
            it.id == baseLanguage?.id
          )
        )
      },
      nextCursor = nextCursor,
    )
  }
}
