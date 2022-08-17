package io.tolgee.configuration

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class MfaConfiguration {
  @Bean
  fun totpGenerator(): TimeBasedOneTimePasswordGenerator {
    return TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30L), 6, OTP_ALGORITHM)
  }

  companion object {
    const val OTP_ALGORITHM = TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1
  }
}
