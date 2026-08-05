package io.tolgee.hateoas.key

import io.tolgee.api.v2.controllers.keys.KeyController
import io.tolgee.service.key.KeySearchResultView
import io.tolgee.service.security.SecurityService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeySearchResultModelAssembler(
  private val securityService: SecurityService,
) : RepresentationModelAssemblerSupport<KeySearchResultView, KeySearchSearchResultModel>(
    KeyController::class.java,
    KeySearchSearchResultModel::class.java,
  ) {
  override fun toModel(view: KeySearchResultView): KeySearchSearchResultModel {
    return KeySearchSearchResultModel(
      view,
      deletedByUserUsername = view.deletedByUserUsername?.let(securityService::maskedMemberField),
    )
  }
}
