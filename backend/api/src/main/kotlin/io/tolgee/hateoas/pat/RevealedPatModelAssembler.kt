package io.tolgee.hateoas.pat

import io.tolgee.api.v2.controllers.PatController
import io.tolgee.model.Pat
import io.tolgee.security.PAT_PREFIX
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class RevealedPatModelAssembler(
  private val patModelAssembler: PatModelAssembler,
) : RepresentationModelAssemblerSupport<Pat, RevealedPatModel>(
    PatController::class.java,
    RevealedPatModel::class.java,
  ) {
  override fun toModel(entity: Pat): RevealedPatModel {
    val token = entity.token ?: throw IllegalStateException("Token not regenerated.")
    return RevealedPatModel(
      patModel = patModelAssembler.toModel(entity),
      token = "$PAT_PREFIX$token",
    )
  }
}
