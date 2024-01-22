package io.tolgee.hateoas.pat

import io.tolgee.api.v2.controllers.PatController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.Pat
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PatWithUserModelAssembler(
  private val patModelAssembler: PatModelAssembler,
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<Pat, PatWithUserModel>(
    PatController::class.java,
    PatWithUserModel::class.java,
  ) {
  override fun toModel(entity: Pat): PatWithUserModel {
    return PatWithUserModel(
      patModel = patModelAssembler.toModel(entity),
      user = simpleUserAccountModelAssembler.toModel(entity.userAccount),
    )
  }
}
