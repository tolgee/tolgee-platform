package io.tolgee.service.translation

import io.tolgee.constants.Message
import io.tolgee.events.OnTranslationsSet
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Language
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.security.ProjectHolder
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import org.springframework.context.ApplicationContext

class SetTranslationTextUtil(
  private val applicationContext: ApplicationContext,
) {
  fun setForKey(
    key: Key,
    translations: Map<String, String?>,
    options: Options? = null,
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
      normalized
        .map { languageByTagFromLanguages(it.key, languages) to it.value }
        .toMap(),
      oldTranslations,
      options,
    ).mapKeys { it.key.tag }
  }

  fun setForKey(
    key: Key,
    translations: Map<Language, String?>,
    oldTranslations: Map<Language, String?>,
    options: Options? = null,
  ): Map<Language, Translation> {
    val result =
      translations.entries
        .associate { (language, value) ->
          language to setTranslationText(key, language, value, options)
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
    options: Options? = null,
  ): Translation {
    val translation = translationService.getOrCreate(key, language)
    setTranslationText(translation, text, options)
    key.translations.add(translation)
    return translation
  }

  fun setTranslationText(
    translation: Translation,
    text: String?,
    options: Options? = null,
  ): Translation {
    setTranslationTextNoSave(translation, text, options)
    return translationService.save(translation)
  }

  fun setTranslationTextNoSave(
    translation: Translation,
    text: String?,
    options: Options? = null,
  ) {
    val hasTextChanged = translation.text != text
    val project = projectHolder.projectOrNull

    if (
      hasTextChanged &&
      project !== null &&
      translation.state == TranslationState.REVIEWED &&
      !securityService.canEditReviewedTranslation(project.id, translation.language.id, translation.key.id)
    ) {
      throw PermissionException(missingScopes = listOf(Scope.TRANSLATIONS_STATE_EDIT))
    }

    if (hasTextChanged) {
      translation.resetFlags()
    }

    translation.text = text

    val hasText = !text.isNullOrEmpty()

    val keepState =
      options?.keepState
        ?: (project?.translationProtection == TranslationProtection.PROTECT_REVIEWED)

    translation.state =
      when {
        translation.state == TranslationState.DISABLED -> TranslationState.DISABLED
        text.isNullOrEmpty() -> TranslationState.UNTRANSLATED
        translation.isUntranslated && hasText -> TranslationState.TRANSLATED
        hasTextChanged ->
          if (keepState) {
            translation.state
          } else {
            TranslationState.TRANSLATED
          }

        else -> translation.state
      }
  }

  private val translationService by lazy {
    applicationContext.getBean(TranslationService::class.java)
  }

  private val languageService by lazy {
    applicationContext.getBean(LanguageService::class.java)
  }

  private val securityService by lazy {
    applicationContext.getBean(SecurityService::class.java)
  }

  private val projectHolder: ProjectHolder by lazy {
    applicationContext.getBean(ProjectHolder::class.java)
  }

  private fun languageByIdFromLanguages(
    id: Long,
    languages: Set<Language>,
  ) = languages.find { it.id == id } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

  private fun languageByTagFromLanguages(
    tag: String,
    languages: Collection<Language>,
  ) = languages.find { it.tag == tag } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

  companion object {
    data class Options(
      val keepState: Boolean = false,
    )
  }
}
