package io.tolgee.api.v2.controllers.translation

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.formats.convertToPluralIfAnyIsPlural
import io.tolgee.hateoas.translations.SetTranslationsResponseModel
import io.tolgee.hateoas.translations.TranslationModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import jakarta.validation.Valid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody

@Service
class CreateOrUpdateTranslationsFacade(
  private val keyService: KeyService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val activityHolder: ActivityHolder,
  private val translationService: TranslationService,
  private val translationModelAssembler: TranslationModelAssembler,
) {
  @Transactional
  fun createOrUpdateTranslations(
    @RequestBody @Valid
    dto: SetTranslationsWithKeyDto,
  ): SetTranslationsResponseModel {
    val key = keyService.find(projectHolder.projectEntity.id, dto.key, dto.namespace) ?: return create(dto)
    return setTranslations(dto, key)
  }

  private fun create(dto: SetTranslationsWithKeyDto): SetTranslationsResponseModel {
    securityService.checkProjectPermission(projectHolder.project.id, Scope.KEYS_EDIT)
    activityHolder.activity = ActivityType.CREATE_KEY
    val key = keyService.create(projectHolder.projectEntity, dto.key, dto.namespace)
    val convertedToPlurals = dto.translations.convertToPluralIfAnyIsPlural()
    if (convertedToPlurals != null) {
      key.isPlural = true
      key.pluralArgName = convertedToPlurals.argName
      keyService.save(key)
    }

    val translations =
      translationService
        .setForKey(key, convertedToPlurals?.convertedStrings ?: dto.translations)
    return getSetTranslationsResponse(key, translations)
  }

  private fun getSetTranslationsResponse(
    key: Key,
    translations: Map<String, Translation>,
  ): SetTranslationsResponseModel {
    return SetTranslationsResponseModel(
      keyId = key.id,
      keyName = key.name,
      keyNamespace = key.namespace?.name,
      keyIsPlural = key.isPlural,
      translations =
        translations.entries.associate { (languageTag, translation) ->
          languageTag to translationModelAssembler.toModel(translation)
        },
    )
  }

  fun setTranslations(
    @RequestBody @Valid
    dto: SetTranslationsWithKeyDto,
    key: Key? = null,
  ): SetTranslationsResponseModel {
    val keyNotNull = key ?: keyService.get(projectHolder.project.id, dto.key, dto.namespace)
    securityService.checkLanguageTranslatePermissionsByTag(
      dto.translations.keys,
      projectHolder.project.id,
      keyNotNull.id,
    )

    val modifiedTranslations = translationService.setForKey(keyNotNull, dto.translations)

    val translations =
      dto.languagesToReturn
        ?.let { languagesToReturn ->
          translationService.findForKeyByLanguages(keyNotNull, languagesToReturn)
            .associateBy { it.language.tag }
        }
        ?: modifiedTranslations

    return getSetTranslationsResponse(keyNotNull, translations)
  }
}
