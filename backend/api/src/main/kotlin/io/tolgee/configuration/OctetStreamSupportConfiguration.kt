package io.tolgee.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

@Configuration
class OctetStreamSupportConfiguration {
  @Bean
  fun octetStreamJsonConverter(): MappingJackson2HttpMessageConverter {
    val converter = MappingJackson2HttpMessageConverter()
    converter.supportedMediaTypes = listOf(MediaType("application", "octet-stream"))
    return converter
  }
}
