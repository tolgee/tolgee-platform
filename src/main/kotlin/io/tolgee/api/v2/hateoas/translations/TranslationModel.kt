package io.tolgee.api.v2.hateoas.translations

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "translations", itemRelation = "translation")
open class TranslationModel(val id: Long, val text: String?) : RepresentationModel<TranslationModel>()
