package io.tolgee.model

import org.hibernate.annotations.ColumnDefault
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

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
