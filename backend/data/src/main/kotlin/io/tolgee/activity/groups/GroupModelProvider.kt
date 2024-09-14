package io.tolgee.activity.groups

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GroupModelProvider<GroupModel, ViewItem> {
  fun provideGroup(groupIds: List<Long>): Map<Long, GroupModel>

  fun provideItems(
    groupId: Long,
    pageable: Pageable,
  ): Page<ViewItem>
}
