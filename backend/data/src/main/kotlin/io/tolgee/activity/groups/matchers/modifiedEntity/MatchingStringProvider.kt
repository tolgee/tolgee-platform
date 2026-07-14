package io.tolgee.activity.groups.matchers.modifiedEntity

import org.jooq.Field

interface MatchingStringProvider {
  fun provide(context: StoringContext): String?

  fun provide(context: SqlContext): Field<String>?
}
