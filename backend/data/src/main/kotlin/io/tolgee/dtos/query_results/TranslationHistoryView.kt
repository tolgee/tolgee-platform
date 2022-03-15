package io.tolgee.dtos.query_results

import io.tolgee.constants.MtServiceType
import io.tolgee.model.enums.TranslationState
import org.hibernate.envers.RevisionType

class TranslationHistoryView {
  var text: String? = null
  var state: TranslationState? = null
  var auto: Boolean? = null
  var mtProvider: MtServiceType? = null
  var timestamp: Long = 0
  var authorName: String? = null
  var authorAvatarHash: String? = null
  var authorEmail: String? = null
  var authorId: Long? = null
  var revisionType: RevisionType = RevisionType.ADD
}
