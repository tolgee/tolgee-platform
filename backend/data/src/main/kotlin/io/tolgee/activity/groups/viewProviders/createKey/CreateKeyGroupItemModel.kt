package io.tolgee.activity.groups.viewProviders.createKey

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.IKeyModel
import io.tolgee.sharedDocs.Key
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "items", itemRelation = "item")
class CreateKeyGroupItemModel(
  override val id: Long,
  override val name: String,
  override val namespace: String?,
  @Schema(description = Key.IS_PLURAL_FIELD)
  val isPlural: Boolean,
  @Schema(description = Key.PLURAL_ARG_NAME_FIELD)
  val pluralArgName: String?,
  @Schema(description = "The base translation value entered when key was created")
  val baseTranslationText: String?,
  val tags: List<String>,
  override val description: String?,
  override val custom: Map<String, Any?>?,
  val baseLanguageId: Long?,
) : IKeyModel
