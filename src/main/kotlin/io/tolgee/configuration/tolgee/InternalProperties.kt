package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.internal")
class InternalProperties {
  var populate: Boolean = false
  var controllerEnabled = false
  var fakeGithubLogin = false
  var showVersion: Boolean = false
}
