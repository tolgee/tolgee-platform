package io.polygloat.configuration.polygloat

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "polygloat")
@ConstructorBinding
open class PolygloatProperties(
        var authentication: AuthenticationProperties = AuthenticationProperties(),
        var smtp: SmtpProperties = SmtpProperties(),
        var sentry: SentryProperties = SentryProperties(),
        var internal: InternalProperties = InternalProperties(),
        var dataPath: String = """${System.getProperty("user.home")}/.polygloat""",
        var screenshotsUrl: String = "/screenshots",
        var maxUploadFileSize: Int = 2048
)