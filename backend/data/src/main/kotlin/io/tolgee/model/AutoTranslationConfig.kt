package io.tolgee.model

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import org.hibernate.annotations.ColumnDefault

@Entity
class AutoTranslationConfig : StandardAuditModel() {
  @ManyToOne
  lateinit var project: Project

  @OneToOne(optional = true)
  var targetLanguage: Language? = null

  var usingTm: Boolean = false

  var usingPrimaryMtService: Boolean = false

  @ColumnDefault("false")
  var enableForImport: Boolean = false
}
