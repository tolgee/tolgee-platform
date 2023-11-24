package io.tolgee.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfiguration : WebMvcConfigurer {
  override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>?>) {
    converters.removeIf { it is MappingJackson2XmlHttpMessageConverter }
  }
}
