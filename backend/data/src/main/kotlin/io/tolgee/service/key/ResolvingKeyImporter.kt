package io.tolgee.service.key

import io.tolgee.constants.Message
import io.tolgee.dtos.KeyImportResolvableResult
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.ScreenshotInfoDto
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportTranslationResolution
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportTranslationResolvableDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.formats.convertToIcuPlurals
import io.tolgee.formats.convertToPluralIfAnyIsPlural
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.Screenshot
import io.tolgee.model.UploadedImage
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Translation
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.equalNullable
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import org.springframework.context.ApplicationContext
import java.io.Serializable

class ResolvingKeyImporter(
  val applicationContext: ApplicationContext,
  val keysToImport: List<ImportKeysResolvableItemDto>,
  val projectEntity: Project,
) {
  private val entityManager = applicationContext.getBean(EntityManager::class.java)
  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val languageService = applicationContext.getBean(LanguageService::class.java)
  private val translationService = applicationContext.getBean(TranslationService::class.java)
  private val screenshotService = applicationContext.getBean(ScreenshotService::class.java)
  private val imageUploadService = applicationContext.getBean(ImageUploadService::class.java)
  private val authenticationFacade = applicationContext.getBean(AuthenticationFacade::class.java)
  private val securityService = applicationContext.getBean(SecurityService::class.java)

  private val errors = mutableListOf<List<Serializable?>>()
  private var importedKeys: List<Key> = emptyList()
  private val updatedTranslationIds = mutableListOf<Long>()
  private val isPluralChangedForKeys = mutableMapOf<Long, String>()
  private val outdatedKeys: MutableList<Long> = mutableListOf()

  operator fun invoke(): KeyImportResolvableResult {
    importedKeys = tryImport()
    checkErrors()
    val screenshots = importScreenshots()
    val keyViews = keyService.getViewsByKeyIds(importedKeys.map { it.id })
    return KeyImportResolvableResult(keyViews, screenshots)
  }

  private fun tryImport(): List<Key> {
    checkLanguagePermissions(keysToImport)

    val result =
      keysToImport.map keys@{ keyToImport ->
        val (key, isKeyNew) = getOrCreateKey(keyToImport)
        val isExistingKeyPlural = key.isPlural
        val translationsToModify = mutableListOf<TranslationToModify>()

        keyToImport.mapLanguageAsKey().forEach translations@{ (language, resolvable) ->
          language ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

          val existingTranslation = getExistingTranslation(key, language.tag)

          val isEmpty = existingTranslation !== null && existingTranslation.text.isNullOrEmpty()

          val isNew = existingTranslation == null

          val translationExists = !isEmpty && !isNew

          if (validate(translationExists, resolvable, key, language.tag)) return@translations

          if (language.base) {
            if (isNew || existingTranslation?.text != resolvable.text) {
              outdatedKeys.add(key.id)
            }
          }

          if (resolvable.resolution == ImportTranslationResolution.FORCE_OVERRIDE) {
            val updated =
              forceAddOrUpdateTranslation(
                key,
                language,
                resolvable.text,
                existingTranslation,
              )
            translationsToModify.add(updated)
            return@translations
          }

          if (isEmpty || (!isNew && resolvable.resolution == ImportTranslationResolution.OVERRIDE)) {
            translationsToModify.add(TranslationToModify(existingTranslation!!, resolvable.text, false))
            return@translations
          }

          if (isNew) {
            val translation =
              Translation(resolvable.text).apply {
                this.key = key
                this.language = entityManager.getReference(Language::class.java, language.id)
              }
            translationsToModify.add(TranslationToModify(translation, resolvable.text, true))
          }
        }

        handlePluralizationAndSave(isExistingKeyPlural, translationsToModify, key)
        key
      }

    translationService.onKeyIsPluralChanged(isPluralChangedForKeys, true, updatedTranslationIds)
    translationService.setOutdatedBatch(outdatedKeys)

    return result
  }

  private fun forceAddOrUpdateTranslation(
    key: Key,
    language: LanguageDto,
    translationText: String,
    existingTranslation: Translation?,
  ): TranslationToModify {
    if (existingTranslation == null) {
      val translation =
        Translation(translationText).apply {
          this.key = key
          this.language = entityManager.getReference(Language::class.java, language.id)
        }
      return TranslationToModify(translation, translationText, true)
    }

    return TranslationToModify(existingTranslation, translationText, false)
  }

  private fun handlePluralizationAndSave(
    isExistingKeyPlural: Boolean,
    translationsToModify: MutableList<TranslationToModify>,
    key: Key,
  ) {
    val translationsToModifyMap = translationsToModify.associateWith { it.text }

    // when existing key is plural, we are converting all to plurals
    if (isExistingKeyPlural) {
      translationsToModifyMap.convertToIcuPlurals(null).convertedStrings.forEach {
        it.key.text = it.value
      }
      translationsToModify.save()
      return
    }

    val convertedToPlurals =
      translationsToModifyMap.convertToPluralIfAnyIsPlural()

    // if anything from the new translations is plural, we are converting the key to plural
    if (convertedToPlurals != null) {
      key.isPlural = true
      keyService.save(key)
      translationsToModify.forEach { translation ->
        translation.text = convertedToPlurals.convertedStrings[translation]
      }
      // now we have to also handle translations of keys,
      // which are already existing in the database
      isPluralChangedForKeys[key.id] = convertedToPlurals.argName
    }

    translationsToModify.save()
  }

  private fun List<TranslationToModify>.save() {
    this.forEach {
      translationService.setTranslation(it.translation, it.text)
      updatedTranslationIds.add(it.translation.id)
    }
  }

  class TranslationToModify(
    val translation: Translation,
    var text: String?,
    val isNew: Boolean,
  )

  private fun importScreenshots(): Map<Long, Screenshot> {
    val uploadedImagesIds =
      keysToImport.flatMap {
        it.screenshots?.map { screenshot -> screenshot.uploadedImageId } ?: listOf()
      }

    val images = imageUploadService.find(uploadedImagesIds)
    checkImageUploadPermissions(images)

    val createdScreenshots =
      images.associate {
        it.id to screenshotService.saveScreenshot(it)
      }

    val locations = images.map { it.location }

    val allReferences =
      screenshotService
        .getKeyScreenshotReferences(
          importedKeys,
          locations,
        ).toMutableList()

    val referencesToDelete = mutableListOf<KeyScreenshotReference>()

    keysToImport.forEach {
      val key = getOrCreateKey(it)
      it.screenshots?.forEach { screenshot ->
        val screenshotResult =
          createdScreenshots[screenshot.uploadedImageId]
            ?: throw NotFoundException(Message.ONE_OR_MORE_IMAGES_NOT_FOUND)
        val info = ScreenshotInfoDto(screenshot.text, screenshot.positions)

        screenshotService.addReference(
          key = key.first,
          screenshot = screenshotResult.screenshot,
          info = info,
          originalDimension = screenshotResult.originalDimension,
          targetDimension = screenshotResult.targetDimension,
        )

        val toDelete =
          allReferences.filter { reference ->
            reference.key.id == key.first.id &&
              reference.screenshot.location == screenshotResult.screenshot.location
          }

        referencesToDelete.addAll(toDelete)
      }
    }

    screenshotService.removeScreenshotReferences(referencesToDelete)

    return createdScreenshots
      .map { (uploadedImageId, screenshotResult) ->
        uploadedImageId to screenshotResult.screenshot
      }.toMap()
  }

  private fun checkImageUploadPermissions(images: List<UploadedImage>) {
    if (images.isNotEmpty()) {
      securityService.checkScreenshotsUploadPermission(projectEntity.id)
    }
    images.forEach { image ->
      if (authenticationFacade.authenticatedUser.id != image.userAccount.id) {
        throw PermissionException(Message.CURRENT_USER_DOES_NOT_OWN_IMAGE)
      }
    }
  }

  private fun checkLanguagePermissions(keys: List<ImportKeysResolvableItemDto>) {
    val languageTags = keys.flatMap { it.translations.keys }
    if (languageTags.isEmpty()) {
      return
    }
    securityService.checkLanguageTranslatePermissionByTag(projectEntity.id, languageTags)
  }

  private fun checkErrors() {
    if (errors.isNotEmpty()) {
      @Suppress("UNCHECKED_CAST")
      throw BadRequestException(Message.IMPORT_KEYS_ERROR, errors as List<Serializable>)
    }
  }

  private fun validate(
    translationExists: Boolean,
    resolvable: ImportTranslationResolvableDto,
    key: Key,
    languageTag: String,
  ): Boolean {
    if (resolvable.resolution == ImportTranslationResolution.FORCE_OVERRIDE) {
      return false
    }

    if (translationExists && resolvable.resolution == ImportTranslationResolution.NEW) {
      errors.add(
        listOf(Message.TRANSLATION_EXISTS.code, key.namespace?.name, key.name, languageTag),
      )
      return true
    }

    if (!translationExists && resolvable.resolution != ImportTranslationResolution.NEW) {
      errors.add(
        listOf(Message.TRANSLATION_NOT_FOUND.code, key.namespace?.name, key.name, languageTag),
      )
      return true
    }
    return false
  }

  private fun getExistingTranslation(
    key: Key,
    languageTag: String,
  ) = existingTranslations[key.namespace?.name to key.name]?.get(languageTag)

  private fun ImportKeysResolvableItemDto.mapLanguageAsKey() =
    translations.mapNotNull { (languageTag, value) ->
      value ?: return@mapNotNull null
      languages[languageTag] to value
    }

  private fun getOrCreateKey(keyToImport: ImportKeysResolvableItemDto): Pair<Key, Boolean> {
    var isNew = false
    val key =
      existingKeys.computeIfAbsent(keyToImport.namespace to keyToImport.name) {
        isNew = true
        securityService.checkProjectPermission(projectEntity.id, Scope.KEYS_CREATE)
        keyService.createWithoutExistenceCheck(
          project = projectEntity,
          name = keyToImport.name,
          namespace = keyToImport.namespace,
          isPlural = false,
        )
      }
    return key to isNew
  }

  private fun getAllByNamespaceAndName(
    projectId: Long,
    keys: List<Pair<String?, String?>>,
  ): List<Key> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Key::class.java)
    val root = query.from(Key::class.java)

    @Suppress("UNCHECKED_CAST")
    val namespaceJoin: Join<Key, Namespace> = root.fetch(Key_.namespace, JoinType.LEFT) as Join<Key, Namespace>

    val predicates =
      keys
        .map { (namespace, name) ->
          cb.and(
            cb.equal(root.get(Key_.name), name),
            cb.equalNullable(namespaceJoin.get(Namespace_.name), namespace),
          )
        }.toTypedArray()

    val projectIdPath = root.get(Key_.project).get(Project_.id)

    query.where(cb.and(cb.equal(projectIdPath, projectId), cb.or(*predicates)))

    return this.entityManager.createQuery(query).resultList
  }

  private val existingKeys by lazy {
    this
      .getAllByNamespaceAndName(
        projectId = projectEntity.id,
        keys = keysToImport.map { it.namespace to it.name },
      ).associateBy { (it.namespace?.name to it.name) }
      .toMutableMap()
  }

  private val languages by lazy {
    val tags = keysToImport.flatMap { it.translations.keys }.toSet()
    languageService.findByTags(tags, projectEntity.id).associateBy { it.tag }
  }

  private val keyLanguagesMap by lazy {
    keysToImport
      .mapNotNull {
        val key = existingKeys[it.namespace to it.name] ?: return@mapNotNull null
        val keyLanguages = it.translations.keys.mapNotNull { tag -> languages[tag] }
        key to keyLanguages
      }.toMap()
  }

  private val existingTranslations by lazy {
    translationService
      .get(keyLanguagesMap)
      .groupBy { it.key.namespace?.name to it.key.name }
      .map { (key, translations) ->
        key to translations.associateBy { it.language.tag }
      }.toMap()
  }
}
