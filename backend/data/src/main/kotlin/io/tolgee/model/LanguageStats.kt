/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
class LanguageStats(
  @OneToOne
  val language: Language
) : StandardAuditModel() {

  var untranslatedWords: Long = 0

  var translatedWords: Long = 0

  var reviewedWords: Long = 0

  var untranslatedKeys: Long = 0

  var translatedKeys: Long = 0

  var reviewedKeys: Long = 0

  var untranslatedPercentage: Double = 0.0

  var translatedPercentage: Double = 0.0

  var reviewedPercentage: Double = 0.0
}
