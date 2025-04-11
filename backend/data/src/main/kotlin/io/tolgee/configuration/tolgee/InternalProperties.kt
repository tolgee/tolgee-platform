package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.internal")
class InternalProperties {
  var controllerEnabled = false
  var fakeThirdPartyLogin = false
  var showVersion: Boolean = false
  var fakeMtProviders: Boolean = false
  var fakeLlmProviders: Boolean = false

  /**
   * When true it fakes checking user sso token for validity
   * When false it pretends failure
   * When null, it doesn't bypass anything
   */
  var verifySsoAccountAvailableBypass: Boolean? = null

  /**
   * Stops server right after it's started.
   * This is used for database changelog generation.
   * We need to apply the schema and then exit to run the schema generator.
   */
  var stopRightAfterStart: Boolean = false

  @E2eRuntimeMutable
  var webhookControllerStatus: Int = 200

  /**
   * When true it fakes storing to the content storage and pretends it's stored.
   * When false it pretends error when storing to the content storage.
   * When null, it doesn't bypass anything
   */
  @E2eRuntimeMutable
  var e3eContentStorageBypassOk: Boolean? = null

  var disableInitialUserCreation: Boolean = false

  var useInMemoryFileStorage: Boolean = false
}
