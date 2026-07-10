package io.tolgee.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class OctetStreamSupportConfiguration : WebMvcConfigurer {
  override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    converters
      .filterIsInstance<JacksonJsonHttpMessageConverter>()
      .forEach { converter ->
        val supportedMediaTypes = converter.supportedMediaTypes.toMutableList()
        supportedMediaTypes.add(MediaType("application", "octet-stream"))
        converter.supportedMediaTypes = supportedMediaTypes
      }
  }
}
