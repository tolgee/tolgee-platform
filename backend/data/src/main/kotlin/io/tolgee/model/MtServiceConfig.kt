package io.tolgee.model

import io.tolgee.constants.MtServiceType
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne

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
