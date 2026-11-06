package io.tolgee.dtos.queryResults

import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import io.tolgee.model.enums.SystemActor
import java.util.Date

interface TranslationHistoryView {
  var modifications: Map<String, PropertyModification>?
  var timestamp: Date
  var authorName: String?
  var authorAvatarHash: String?
  var authorEmail: String?
  var authorDeletedAt: Date?
  var authorId: Long?
  var revisionType: RevisionType
  var systemActor: SystemActor?
}
