package io.tolgee.configuration.tolgee.machineTranslation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.machine-translation.tolgee")
open class TolgeeMachineTranslationProperties(
  override var defaultEnabled: Boolean = true,
  override var defaultPrimary: Boolean = false,
  var url: String? = null,
  var batchMaxTokensPerMinute: Long = 100000,
  var batchMaxCallsPerMinute: Long = 500,
  var tokensToPreConsume: Long = 1000,
  /**
   * How many mtCredits are consumed for one token
   * Mt credits in db are stored as real credits * 100
   *
   * So to compute real credits, use this formula:
   * realCredits = <Tokens consumed by Translator> * tokensToMtCredits / 100
   */
  var tokensToMtCredits: Double = 10.0,
) : MachineTranslationServiceProperties
