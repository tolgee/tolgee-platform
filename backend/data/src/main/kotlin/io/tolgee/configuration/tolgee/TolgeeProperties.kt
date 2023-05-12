/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "tolgee")
@ConstructorBinding
@Schema(description = "Configuration specific to Tolgee")
open class TolgeeProperties(
  var authentication: AuthenticationProperties = AuthenticationProperties(),
  var smtp: SmtpProperties = SmtpProperties(),
  var sentry: SentryProperties = SentryProperties(),
  var chatwootToken: String? = null,
  var capterraTracker: String? = null,
  var ga4Tag: String? = null,
  var internal: InternalProperties = InternalProperties(),
  @Schema(description = "Public base path where files are accessible. Used by the user interface. (default: same origin, at the root).")
  var fileStorageUrl: String = "",
  @Schema(description = "Maximum size of uploaded files (in kilobytes).", defaultValue = "`2048` ≈ 2MB")
  var maxUploadFileSize: Int = 51200,
  @Schema(description = "Maximum amount of screenshots which can be uploaded per API key.")
  val maxScreenshotsPerKey: Int = 20,
  var fileStorage: FileStorageProperties = FileStorageProperties(),
  @Schema(description = "Public URL where Tolgee is accessible. Used to generate links to Tolgee (e.g. email confirmation link).")
  var frontEndUrl: String? = null,
  var websocket: WebsocketProperties = WebsocketProperties(),
  var appName: String = "Tolgee",
  @Schema(description = "Maximum length of translations.")
  open var maxTranslationTextLength: Long = 10000,
  var cache: CacheProperties = CacheProperties(),
  var recaptcha: ReCaptchaProperties = ReCaptchaProperties(),
  var machineTranslation: MachineTranslationProperties = MachineTranslationProperties(),
  var postgresAutostart: PostgresAutostartProperties = PostgresAutostartProperties(),
  var sendInBlueProperties: SendInBlueProperties = SendInBlueProperties(),
  open var import: ImportProperties = ImportProperties(),
  var rateLimitProperties: RateLimitProperties = RateLimitProperties()
)
