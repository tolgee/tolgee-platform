package io.tolgee.activity.groups.viewProviders.setTranslations

import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.GroupModelProvider
import io.tolgee.activity.groups.dataProviders.GroupDataProvider
import io.tolgee.model.translation.Translation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class SetTranslationsGroupModelProvider(
  private val groupDataProvider: GroupDataProvider,
) :
  GroupModelProvider<SetTranslationsGroupModel, SetTranslationsGroupItemModel> {
  override fun provideGroup(groupIds: List<Long>): Map<Long, SetTranslationsGroupModel> {
    val translationCounts =
      groupDataProvider.provideCounts(
        ActivityGroupType.SET_TRANSLATIONS,
        groupIds = groupIds,
        entityClass = Translation::class,
      )

    return groupIds.associateWith {
      SetTranslationsGroupModel(
        translationCounts[it] ?: 0,
      )
    }
  }

  override fun provideItems(
    groupId: Long,
    pageable: Pageable,
  ): Page<SetTranslationsGroupItemModel> {
    return Page.empty()
  }
}
