package io.tolgee.configuration

import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomPrettyPrinterConfiguration {
  @Bean
  fun customPrettyPrinter(): CustomPrettyPrinter {
    return CustomPrettyPrinter()
  }
}
