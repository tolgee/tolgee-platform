package io.tolgee.hateoas.translationMemory

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "translationMemoryItems", itemRelation = "translationMemoryItem")
open class TranslationMemoryItemModel(
  var targetText: String,
  var baseText: String,
  var keyName: String,
  var similarity: Float,
) : RepresentationModel<TranslationMemoryItemModel>()
