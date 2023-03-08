package io.tolgee.configuration.tolgee.machineTranslation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation.baidu")
open class BaiduMachineTranslationProperties(
  override var defaultEnabled: Boolean = true,
  override var defaultPrimary: Boolean = false,
  var appId: String? = null,
  var appSecret: String? = null,
  var action: Boolean = false,
) : MachineTranslationServiceProperties
