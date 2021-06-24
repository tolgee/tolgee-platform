package io.tolgee.api.v2.hateoas.translations

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class KeyWithTranslationsModel(
        val keyId: Long,
        val keyName: String,
        val translations: Map<String, TranslationModel>
) : RepresentationModel<KeyWithTranslationsModel>()
