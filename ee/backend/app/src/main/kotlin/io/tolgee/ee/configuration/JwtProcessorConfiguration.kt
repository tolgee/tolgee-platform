package io.tolgee.ee.configuration

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JwtProcessorConfiguration {
  @Bean
  fun jwtProcessor(): ConfigurableJWTProcessor<SecurityContext> = DefaultJWTProcessor()
}
