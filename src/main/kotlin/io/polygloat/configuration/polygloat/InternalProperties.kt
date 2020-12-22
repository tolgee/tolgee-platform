package io.polygloat.configuration.polygloat

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polygloat.internal")
class InternalProperties {
    var populate: Boolean = false
    var controllerEnabled = false
    var fakeEmailsSent = false
}
