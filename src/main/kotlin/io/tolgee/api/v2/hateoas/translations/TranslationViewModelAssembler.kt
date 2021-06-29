package io.tolgee.api.v2.hateoas.translations

import io.tolgee.api.v2.controllers.translation.V2TranslationsController
import io.tolgee.model.views.TranslationView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationViewModelAssembler : RepresentationModelAssemblerSupport<TranslationView, TranslationModel>(
        V2TranslationsController::class.java, TranslationModel::class.java) {
    override fun toModel(entity: TranslationView): TranslationModel {
        return TranslationModel(id = entity.id, text = entity.text, state = entity.state)
    }
}
