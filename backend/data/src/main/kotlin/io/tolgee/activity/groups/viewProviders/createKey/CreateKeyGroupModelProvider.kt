package io.tolgee.activity.groups.viewProviders.createKey

import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.GroupModelProvider
import io.tolgee.activity.groups.data.DescribingMapping
import io.tolgee.activity.groups.data.RelatedMapping
import io.tolgee.activity.groups.dataProviders.GroupDataProvider
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.translation.Translation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CreateKeyGroupModelProvider(
  private val groupDataProvider: GroupDataProvider,
) :
  GroupModelProvider<CreateKeyGroupModel, CreateKeyGroupItemModel> {
  override fun provideGroup(groupIds: List<Long>): Map<Long, CreateKeyGroupModel> {
    val keyCounts =
      groupDataProvider.provideCounts(ActivityGroupType.CREATE_KEY, groupIds = groupIds, entityClass = Key::class)

    return groupIds.associateWith {
      CreateKeyGroupModel(
        keyCounts[it] ?: 0,
      )
    }
  }

  override fun provideItems(
    groupId: Long,
    pageable: Pageable,
  ): Page<CreateKeyGroupItemModel> {
    val entities =
      groupDataProvider.provideRelevantModifiedEntities(
        ActivityGroupType.CREATE_KEY,
        Key::class,
        groupId,
        pageable,
      )

    val translationMapping =
      RelatedMapping(
        entityClass = Translation::class,
        field = "key",
        entities,
      )

    val keyMetaMapping =
      RelatedMapping(
        entityClass = KeyMeta::class,
        field = "key",
        entities,
      )

    val relatedEntities =
      groupDataProvider.getRelatedEntities(
        groupType = ActivityGroupType.CREATE_KEY,
        relatedMappings =
          listOf(
            keyMetaMapping,
            translationMapping,
          ),
        groupId = groupId,
      )

    val descriptions =
      groupDataProvider.getDescribingEntities(
        entities,
        listOf(DescribingMapping(Key::class, Key::namespace.name)),
      )

    return entities.map { entity ->
      val baseTranslation =
        relatedEntities[entity]
          ?.get(translationMapping)?.singleOrNull()

      val baseTranslationValue =
        baseTranslation?.modifications
          ?.get("text")
          ?.new as? String

      val baseLanguageId = baseTranslation?.describingRelations?.get("language")?.entityId

      val keyMeta =
        relatedEntities[entity]
          ?.get(keyMetaMapping)
          ?.singleOrNull()

      CreateKeyGroupItemModel(
        entity.entityId,
        name = entity.getFieldFromView(Key::name.name),
        tags = (keyMeta?.getFieldFromViewNullable(KeyMeta::tags.name) as? List<String>) ?: emptyList(),
        description = keyMeta?.getFieldFromViewNullable(KeyMeta::description.name),
        custom = keyMeta?.getFieldFromViewNullable(KeyMeta::custom.name),
        isPlural = entity.getFieldFromView(Key::isPlural.name),
        pluralArgName = entity.getFieldFromViewNullable(Key::pluralArgName.name),
        namespace =
          descriptions[entity]
            ?.find { it.entityClass == Namespace::class.simpleName }
            ?.data
            ?.get(Namespace::name.name)
            as? String,
        baseTranslationValue = baseTranslationValue,
        baseLanguageId = baseLanguageId,
      )
    }
  }
}
