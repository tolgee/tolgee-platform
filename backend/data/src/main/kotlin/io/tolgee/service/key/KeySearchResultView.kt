package io.tolgee.service.key

import java.util.Date

interface KeySearchResultView {
  val id: Long
  val namespace: String?
  val name: String
  val baseTranslation: String?
  val translation: String?
  val description: String?
  val deletedAt: Date?
  val plural: Boolean?
}
