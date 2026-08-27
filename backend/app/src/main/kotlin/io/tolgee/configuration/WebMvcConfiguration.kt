package io.tolgee.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfiguration : WebMvcConfigurer {
  // The non-deprecated ServerBuilder API bypasses Spring HATEOAS's HAL converter registration.
  @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
  override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    converters.removeIf { it is JacksonXmlHttpMessageConverter }
  }
}
