package io.tolgee.activity.groups

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GroupModelProvider<GroupModel, ViewItem> {
  fun provideGroupModel(groupIds: List<Long>): Map<Long, GroupModel>

  fun provideItemModel(
    groupId: Long,
    pageable: Pageable,
  ): Page<ViewItem>
}
