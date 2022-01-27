package io.tolgee.model

import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
class AutoTranslationConfig : StandardAuditModel() {
  @OneToOne
  lateinit var project: Project

  var usingTm: Boolean = false

  var usingPrimaryMtService: Boolean = false
}
