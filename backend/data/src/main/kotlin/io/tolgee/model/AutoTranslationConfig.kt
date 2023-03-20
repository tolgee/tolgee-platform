package io.tolgee.model

import jakarta.persistence.Entity
import jakarta.persistence.OneToOne

@Entity
class AutoTranslationConfig : StandardAuditModel() {
  @OneToOne
  lateinit var project: Project

  var usingTm: Boolean = false

  var usingPrimaryMtService: Boolean = false
}
