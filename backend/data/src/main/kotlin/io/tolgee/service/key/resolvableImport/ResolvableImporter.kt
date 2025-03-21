package io.tolgee.service.key.resolvableImport

import io.tolgee.constants.Message
import io.tolgee.dtos.KeyImportResolvableResult
import io.tolgee.dtos.request.ScreenshotInfoDto
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UploadedImage
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Translation
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext
import java.io.Serializable

class ResolvableImporter(
  val applicationContext: ApplicationContext,
  val keysToImport: List<ImportKeysResolvableItemDto>,
  val projectEntity: Project,
) {
  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val translationService = applicationContext.getBean(TranslationService::class.java)
  private val screenshotService = applicationContext.getBean(ScreenshotService::class.java)
  private val imageUploadService = applicationContext.getBean(ImageUploadService::class.java)
  private val authenticationFacade = applicationContext.getBean(AuthenticationFacade::class.java)
  private val securityService = applicationContext.getBean(SecurityService::class.java)

  val context by lazy {
    ResolvableImportContext(applicationContext, keysToImport, projectEntity)
  }

  operator fun invoke(): KeyImportResolvableResult {
    context.importedKeys = tryImport()
    checkErrors()
    val screenshots = importScreenshots()
    val keyViews = keyService.getViewsByKeyIds(context.importedKeys.map { it.id })
    return KeyImportResolvableResult(keyViews, screenshots)
  }

  private fun tryImport(): List<Key> {
    checkLanguagePermissions(keysToImport)

    val result =
      keysToImport.map keys@{ keyToImport ->
        ResolvableKeyImporter(keyToImport, context).import()
      }

    handlePluralMigration()

    translationService.setOutdatedBatch(context.outdatedKeys)
    return result
  }

  private fun handlePluralMigration() {
    val (newPlurals, newNotPlurals) = context.isPluralChangedForKeys.asSequence()
      .map { it.key to it.value }
      .partition { (_, pluralArgName) -> pluralArgName != null }

    translationService.onKeyIsPluralChanged(newPlurals.toMap(), true, context.updatedTranslationIds)
    translationService.onKeyIsPluralChanged(newNotPlurals.toMap(), false, context.updatedTranslationIds)
  }

  class TranslationToModify(
    val translation: Translation,
    var text: String?,
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
          context.importedKeys,
          locations,
        ).toMutableList()

    val referencesToDelete = mutableListOf<KeyScreenshotReference>()

    keysToImport.forEach {
      val key = context.getOrCreateKey(it)
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
    if (context.errors.isNotEmpty()) {
      @Suppress("UNCHECKED_CAST")
      throw BadRequestException(Message.IMPORT_KEYS_ERROR, context.errors as List<Serializable>)
    }
  }
}
