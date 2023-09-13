package io.tolgee.model.mtServiceConfig

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.service.machineTranslation.MtServiceInfo
import org.hibernate.annotations.ColumnDefault
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
  var primaryServiceFormality: Formality? = null

  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = MtServiceType::class)
  var enabledServices: MutableSet<MtServiceType> = mutableSetOf()

  @Enumerated(EnumType.STRING)
  @ColumnDefault("DEFAULT")
  var awsFormality: Formality = Formality.DEFAULT

  @Enumerated(EnumType.STRING)
  @ColumnDefault("DEFAULT")
  var deeplFormality: Formality = Formality.DEFAULT

  @Enumerated(EnumType.STRING)
  @ColumnDefault("DEFAULT")
  var tolgeeFormality: Formality = Formality.DEFAULT

  val primaryServiceInfo: MtServiceInfo?
    get() {
      return MtServiceInfo(primaryService ?: return null, primaryServiceFormality)
    }

  val enabledServicesInfo
    get() = enabledServices.mapNotNull { getServiceInfo(this, it) }

  companion object {
    fun getServiceInfo(
      config: MtServiceConfig,
      serviceType: MtServiceType?
    ): MtServiceInfo? {
      if (serviceType == null) {
        return null
      }
      val formality = when (serviceType) {
        MtServiceType.AWS -> config.awsFormality
        MtServiceType.DEEPL -> config.deeplFormality
        MtServiceType.TOLGEE -> config.tolgeeFormality
        else -> null
      }
      return MtServiceInfo(serviceType, formality)
    }
  }
}
