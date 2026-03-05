package io.tolgee.hateoas.key.trash

import io.tolgee.api.v2.controllers.keys.KeyTrashController
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.hateoas.translations.TranslationViewModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.service.AvatarService
import io.tolgee.service.key.KeyTrashPurgeScheduler
import io.tolgee.util.addDays
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TrashedKeyWithTranslationsModelAssembler(
  private val avatarService: AvatarService,
  private val screenshotModelAssembler: ScreenshotModelAssembler,
) : RepresentationModelAssemblerSupport<KeyWithTranslationsView, TrashedKeyWithTranslationsModel>(
    KeyTrashController::class.java,
    TrashedKeyWithTranslationsModel::class.java,
  ) {
  override fun toModel(view: KeyWithTranslationsView): TrashedKeyWithTranslationsModel {
    val deletedAt = view.deletedAt!!
    val tags = view.keyTags.map { TagModel(it.id, it.name) }
    val translationsMap =
      view.translations.mapValues { (_, tv) ->
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
      view.screenshots?.map { screenshotModelAssembler.toModel(it) } ?: emptyList()

    return TrashedKeyWithTranslationsModel(
      id = view.keyId,
      name = view.keyName,
      namespace = view.keyNamespace,
      deletedAt = deletedAt,
      permanentDeleteAt = deletedAt.addDays(KeyTrashPurgeScheduler.RETENTION_DAYS),
      description = view.keyDescription,
      tags = tags,
      translations = translationsMap,
      screenshots = screenshots,
      isPlural = view.keyIsPlural,
      deletedBy = buildDeletedByModel(view),
    )
  }

  private fun buildDeletedByModel(view: KeyWithTranslationsView): SimpleUserAccountModel? {
    return view.deletedByUserId?.let { userId ->
      SimpleUserAccountModel(
        id = userId,
        username = view.deletedByUserUsername ?: "",
        name = view.deletedByUserName,
        avatar = avatarService.getAvatarLinks(view.deletedByUserAvatarHash),
        deleted = view.deletedByUserDeletedAt != null,
      )
    }
  }
}
