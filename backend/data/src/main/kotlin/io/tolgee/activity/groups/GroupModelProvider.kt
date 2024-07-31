package io.tolgee.activity.groups

interface GroupModelProvider<View> {
  fun provide(groupIds: List<Long>): Map<Long, View>
}
