package io.tolgee.api.v2.hateoas.translations

import io.tolgee.api.v2.controllers.translation.V2TranslationsController
import io.tolgee.api.v2.hateoas.user_account.SimpleUserAccountModel
import io.tolgee.dtos.query_results.TranslationHistoryView
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationHistoryModelAssembler(
  private val avatarService: AvatarService
) : RepresentationModelAssemblerSupport<TranslationHistoryView, TranslationHistoryModel>(
  V2TranslationsController::class.java, TranslationHistoryModel::class.java
) {
  override fun toModel(view: TranslationHistoryView): TranslationHistoryModel {
    val avatar = avatarService.getAvatarLinks(view.authorAvatarHash)

    return TranslationHistoryModel(
      text = view.text,
      state = view.state,
      auto = view.auto,
      mtProvider = view.mtProvider,
      author = view.authorId?.let {
        SimpleUserAccountModel(
          id = it,
          name = view.authorName,
          username = view.authorEmail ?: "",
          avatar = avatar
        )
      },
      timestamp = view.timestamp,
      revisionType = view.revisionType
    )
  }
}
