package io.tolgee.configuration

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration

@Configuration
class SecurityConfiguration {
  @Bean
  fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder()
  }

  @Bean
  fun totpGenerator(): TimeBasedOneTimePasswordGenerator {
    return TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30L), 6, OTP_ALGORITHM)
  }

  companion object {
    const val OTP_ALGORITHM = TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1
  }
}
