/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import javax.persistence.Entity
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["language_id"], name = "language_stats_language_id_key")])
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
