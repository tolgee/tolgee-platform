package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.key.Key

/** One project, one user, one key: the suggestions themselves are inserted by SQL with chosen timestamps. */
class CommunityContributionDayTestData : BaseTestData("counters_author", "counters_project") {
  val key: Key = projectBuilder.addKey { name = "key" }.self
}
