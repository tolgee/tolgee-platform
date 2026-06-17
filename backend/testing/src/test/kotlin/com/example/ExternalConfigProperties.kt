package com.example

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external")
class ExternalConfigProperties {
  var value: String = ""
}
