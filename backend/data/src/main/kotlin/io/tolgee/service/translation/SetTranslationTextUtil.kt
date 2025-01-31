package io.tolgee.service.translation

import io.tolgee.constants.Message
import io.tolgee.events.OnTranslationsSet
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.language.LanguageService
import org.springframework.context.ApplicationContext

class SetTranslationTextUtil(
  private val applicationContext: ApplicationContext,
) {
  fun setForKey(
    key: Key,
    translations: Map<String, String?>,
  ): Map<String, Translation> {
    val normalized =
      translationService.validateAndNormalizePlurals(translations, key.isPlural, key.pluralArgName)
    val languages = languageService.findEntitiesByTags(translations.keys, key.project.id)
    val oldTranslations =
      translationService.getKeyTranslations(languages, key.project, key).associate {
        languageByIdFromLanguages(
          it.language.id,
          languages,
        ) to it.text
      }

    return setForKey(
      key,
      normalized.map { languageByTagFromLanguages(it.key, languages) to it.value }
        .toMap(),
      oldTranslations,
    ).mapKeys { it.key.tag }
  }

  fun setForKey(
    key: Key,
    translations: Map<Language, String?>,
    oldTranslations: Map<Language, String?>,
  ): Map<Language, Translation> {
    val result =
      translations.entries.associate { (language, value) ->
        language to setTranslationText(key, language, value)
      }.mapValues { it.value }

    applicationContext.publishEvent(
      OnTranslationsSet(
        source = this,
        key = key,
        oldValues = oldTranslations.map { it.key.tag to it.value }.toMap(),
        translations = result.values.toList(),
      ),
    )

    return result
  }

  fun setTranslationText(
    key: Key,
    language: Language,
    text: String?,
  ): Translation {
    val translation = translationService.getOrCreate(key, language)
    setTranslationText(translation, text)
    key.translations.add(translation)
    return translation
  }

  fun setTranslationText(
    translation: Translation,
    text: String?,
  ): Translation {
    setTranslationTextNoSave(translation, text)
    return translationService.save(translation)
  }

  fun setTranslationTextNoSave(
    translation: Translation,
    text: String?,
  ) {
    val hasTextChanged = translation.text != text

    if (hasTextChanged) {
      translation.resetFlags()
    }

    translation.text = text

    val hasText = !text.isNullOrEmpty()

    translation.state =
      when {
        translation.state == TranslationState.DISABLED -> TranslationState.DISABLED
        translation.isUntranslated && hasText -> TranslationState.TRANSLATED
        hasTextChanged -> TranslationState.TRANSLATED
        text.isNullOrEmpty() -> TranslationState.UNTRANSLATED
        else -> translation.state
      }
  }

  private val translationService by lazy {
    applicationContext.getBean(TranslationService::class.java)
  }

  private val languageService by lazy {
    applicationContext.getBean(LanguageService::class.java)
  }

  private fun languageByIdFromLanguages(
    id: Long,
    languages: Set<Language>,
  ) = languages.find { it.id == id } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

  private fun languageByTagFromLanguages(
    tag: String,
    languages: Collection<Language>,
  ) = languages.find { it.tag == tag } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
}
