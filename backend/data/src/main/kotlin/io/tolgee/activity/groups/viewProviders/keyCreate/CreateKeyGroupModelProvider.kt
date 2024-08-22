package io.tolgee.activity.groups.viewProviders.keyCreate

import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.GroupModelProvider
import io.tolgee.activity.groups.dataProviders.GroupDataProvider
import io.tolgee.model.key.Key
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CreateKeyGroupModelProvider(
  private val groupDataProvider: GroupDataProvider,
) :
  GroupModelProvider<CreateKeyGroupModel, CreateKeyGroupItemModel> {
  override fun provideGroupModel(groupIds: List<Long>): Map<Long, CreateKeyGroupModel> {
    val keyCounts =
      groupDataProvider.provideCounts(ActivityGroupType.CREATE_KEY, groupIds = groupIds, entityClass = Key::class)

    return groupIds.associateWith {
      CreateKeyGroupModel(
        keyCounts[it] ?: 0,
      )
    }
  }

  override fun provideItemModel(
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

    return entities.map {
      CreateKeyGroupItemModel(
        it.entityId,
        name = it.getFieldFromView("name"),
        tags = it.getFieldFromViewNullable("tags") ?: emptySet(),
        description = it.getFieldFromViewNullable("description"),
        custom = it.getFieldFromViewNullable("custom"),
        isPlural = false,
        pluralArgName = null,
        namespace = null,
        baseTranslationValue = null,
      )
    }
  }
}
