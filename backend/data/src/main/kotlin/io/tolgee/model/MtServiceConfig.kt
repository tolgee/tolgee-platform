package io.tolgee.model

import io.tolgee.constants.MtServiceType
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

@Entity
class MtServiceConfig : StandardAuditModel() {
  @ManyToOne
  lateinit var project: Project

  /**
   * When null, then its default config
   */
  @OneToOne(fetch = FetchType.LAZY)
  var targetLanguage: Language? = null

  @Enumerated(EnumType.STRING)
  var primaryService: MtServiceType? = null

  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = MtServiceType::class)
  var enabledServices: MutableSet<MtServiceType> = mutableSetOf()
}
