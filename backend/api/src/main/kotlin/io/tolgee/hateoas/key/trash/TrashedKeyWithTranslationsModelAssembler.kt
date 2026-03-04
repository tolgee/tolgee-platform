package io.tolgee.hateoas.key.trash

import io.tolgee.api.v2.controllers.keys.KeyTrashController
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.hateoas.screenshot.ScreenshotModel
import io.tolgee.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.hateoas.translations.TranslationViewModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.Screenshot
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.service.AvatarService
import io.tolgee.service.key.KeyTrashPurgeScheduler
import io.tolgee.util.addDays
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport

class TrashedKeyWithTranslationsModelAssembler(
  private val avatarService: AvatarService,
  private val screenshotModelAssembler: ScreenshotModelAssembler,
  private val screenshotsByKeyId: Map<Long, List<Screenshot>>,
) : RepresentationModelAssemblerSupport<KeyWithTranslationsView, TrashedKeyWithTranslationsModel>(
    KeyTrashController::class.java,
    TrashedKeyWithTranslationsModel::class.java,
  ) {
  override fun toModel(entity: KeyWithTranslationsView): TrashedKeyWithTranslationsModel {
    val deletedAt = entity.deletedAt!!
    val tags = entity.keyTags.map { TagModel(it.id, it.name) }
    val translationsMap =
      entity.translations.mapValues { (_, tv) ->
        TranslationViewModel(
          id = tv.id,
          text = tv.text,
          state = tv.state,
          auto = tv.auto,
          outdated = tv.outdated,
          mtProvider = tv.mtProvider,
          commentCount = tv.commentCount,
          unresolvedCommentCount = tv.unresolvedCommentCount,
          labels = null,
          activeSuggestionCount = tv.activeSuggestionCount,
          totalSuggestionCount = tv.totalSuggestionCount,
        )
      }

    val screenshots =
      screenshotsByKeyId[entity.keyId]?.map { screenshotModelAssembler.toModel(it) } ?: emptyList()

    return TrashedKeyWithTranslationsModel(
      id = entity.keyId,
      name = entity.keyName,
      namespace = entity.keyNamespace,
      deletedAt = deletedAt,
      permanentDeleteAt = deletedAt.addDays(KeyTrashPurgeScheduler.RETENTION_DAYS),
      description = entity.keyDescription,
      tags = tags,
      translations = translationsMap,
      screenshots = screenshots,
      isPlural = entity.keyIsPlural,
      deletedBy = buildDeletedByModel(entity),
    )
  }

  private fun buildDeletedByModel(entity: KeyWithTranslationsView): SimpleUserAccountModel? {
    return entity.deletedByUserId?.let { userId ->
      SimpleUserAccountModel(
        id = userId,
        username = entity.deletedByUserUsername ?: "",
        name = entity.deletedByUserName,
        avatar = avatarService.getAvatarLinks(entity.deletedByUserAvatarHash),
        deleted = entity.deletedByUserDeletedAt != null,
      )
    }
  }
}
