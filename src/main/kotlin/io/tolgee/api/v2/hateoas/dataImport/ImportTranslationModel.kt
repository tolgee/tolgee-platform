package io.tolgee.api.v2.hateoas.dataImport

import io.tolgee.model.views.ImportTranslationView
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "translations", itemRelation = "translation")
open class ImportTranslationModel(
        override val id: Long,
        override val text: String,
        override val keyName: String,
        override val keyId: Long,
        override val conflictId: Long?,
        override val conflictText: String?,
        override val override: Boolean,
        override val resolved: Boolean
) : RepresentationModel<ImportTranslationModel>(), ImportTranslationView
