package io.tolgee.model

import io.tolgee.constants.MtServiceType
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.OneToOne

@Entity
class MtServiceConfig : StandardAuditModel() {
  @OneToOne
  lateinit var project: Project

  /**
   * When null, then its default config
   */
  @OneToOne
  var targetLanguage: Language? = null

  @Enumerated(EnumType.STRING)
  var primaryService: MtServiceType? = null

  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = MtServiceType::class)
  var enabledServices: MutableSet<MtServiceType> = mutableSetOf()
}
