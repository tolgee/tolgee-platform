package io.tolgee.model.mtServiceConfig

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.StandardAuditModel
import io.tolgee.service.machineTranslation.MtServiceInfo
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
  ],
)
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

  @ManyToOne
  @JoinColumn(name = "prompt_id")
  var prompt: Prompt? = null

  val primaryServiceInfo: MtServiceInfo?
    get() = primaryService?.let { getServiceInfo(this, it) }

  val enabledServicesInfo
    get() = enabledServices.mapNotNull { getServiceInfo(this, it) }

  companion object {
    fun getServiceInfo(
      config: MtServiceConfig,
      serviceType: MtServiceType?,
    ): MtServiceInfo? {
      if (serviceType == null) {
        return null
      }
      val formality =
        when (serviceType) {
          MtServiceType.AWS -> config.awsFormality
          MtServiceType.DEEPL -> config.deeplFormality
          else -> null
        }
      val promptId =
        when (serviceType) {
          MtServiceType.PROMPT -> config.prompt?.id
          else -> null
        }
      return MtServiceInfo(serviceType, formality, promptId)
    }
  }
}
