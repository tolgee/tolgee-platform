package io.tolgee.dtos.query_results

import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import java.util.*

interface TranslationHistoryView {
  var modifications: Map<String, PropertyModification>?
  var timestamp: Date
  var authorName: String?
  var authorAvatarHash: String?
  var authorEmail: String?
  var authorId: Long?
  var revisionType: RevisionType
}
