/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.AdditionalDocsProperties
import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@AdditionalDocsProperties(
  [
    DocProperty(
      name = "server",
      prefix = "server",
      displayName = "Server settings",
      description = "These properties are used to configure the server.",
      children = [
        DocProperty(
          name = "port",
          description = "Port on which Tolgee exposes itself.",
          defaultValue = "8080"
        )
      ]
    ),
    DocProperty(
      name = "spring",
      prefix = "spring",
      displayName = "Spring settings",
      description = "These properties are used to configure Spring framework.",
      children = [
        DocProperty(
          name = "datasource",
          prefix = "spring.datasource",
          displayName = "Data source settings",
          description = "Since Tolgee is built on Spring framework, you have to edit following configuration props\n" +
            "to configure its database connection. These properties can be omitted when using\n" +
            "[Postgres autostart](#postgres-autostart), which is enabled by default.",
          children = [
            DocProperty(
              name = "url",
              description = "The url of the datasource in format `jdbc:postgresql://<host>:<port>/<dbname>`. " +
                "e.g. `jdbc:postgresql://db:5432/postgres`",
              defaultValue = ""
            ),
            DocProperty(
              name = "username",
              description = "Database username. e.g. `postgres`",
              defaultValue = ""
            ),
            DocProperty(
              name = "password",
              description = "Database password. e.g. `postgres`",
              defaultValue = ""
            ),
          ]
        )
      ]
    )
  ],
  global = true
)
@ConfigurationProperties(prefix = "tolgee")
@DocProperty(description = "Configuration specific to Tolgee.", displayName = "Tolgee")
open class TolgeeProperties(
  var authentication: AuthenticationProperties = AuthenticationProperties(),
  var smtp: SmtpProperties = SmtpProperties(),
  var sentry: SentryProperties = SentryProperties(),

  @DocProperty(hidden = true)
  var chatwootToken: String? = null,

  @DocProperty(hidden = true)
  var capterraTracker: String? = null,

  @DocProperty(hidden = true)
  var ga4Tag: String? = null,

  @DocProperty(hidden = true)
  var internal: InternalProperties = InternalProperties(),

  @DocProperty(
    description = "Public base path where files are accessible. Used by the user interface.",
    defaultExplanation = "The root of the same origin."
  )
  var fileStorageUrl: String = "",

  @DocProperty(description = "Maximum size of uploaded files (in kilobytes).", defaultExplanation = "≈ 50MB")
  var maxUploadFileSize: Int = 51200,

  @DocProperty(description = "Maximum amount of screenshots which can be uploaded per API key.")
  val maxScreenshotsPerKey: Int = 20,

  var fileStorage: FileStorageProperties = FileStorageProperties(),

  @DocProperty(
    description = "Public URL where Tolgee is accessible. " +
      "Used to generate links to Tolgee (e.g. email confirmation link)."
  )
  var frontEndUrl: String? = null,

  var websocket: WebsocketProperties = WebsocketProperties(),

  @DocProperty(description = "Name of the application.", hidden = true)
  var appName: String = "Tolgee",

  @DocProperty(description = "Maximum length of translations.")
  open var maxTranslationTextLength: Long = 10000,

  var cache: CacheProperties = CacheProperties(),
  var recaptcha: ReCaptchaProperties = ReCaptchaProperties(),
  var machineTranslation: MachineTranslationProperties = MachineTranslationProperties(),
  var postgresAutostart: PostgresAutostartProperties = PostgresAutostartProperties(),

  @DocProperty(hidden = true)
  var sendInBlue: SendInBlueProperties = SendInBlueProperties(),

  open var import: ImportProperties = ImportProperties(),
  var rateLimit: RateLimitProperties = RateLimitProperties(),
  @DocProperty(hidden = true)
  var postHog: PostHogProperties = PostHogProperties(),

  var telemetry: TelemetryProperties = TelemetryProperties()
)
