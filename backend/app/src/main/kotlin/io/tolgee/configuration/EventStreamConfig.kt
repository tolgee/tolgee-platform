package io.tolgee.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper

@Configuration
class EventStreamConfig(
  private val objectMapper: ObjectMapper,
) : WebMvcConfigurer {
  // The non-deprecated ServerBuilder API bypasses Spring HATEOAS's HAL converter registration.
  @Suppress("DEPRECATION")
  override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    converters.add(EventStreamHttpMessageConverter(objectMapper))
    converters.add(JavascriptHttpMessageConverter(objectMapper))
  }
}

class EventStreamHttpMessageConverter(
  objectMapper: ObjectMapper,
) : AbstractJacksonHttpMessageConverter<ObjectMapper>(objectMapper, MediaType.TEXT_EVENT_STREAM)

class JavascriptHttpMessageConverter(
  objectMapper: ObjectMapper,
) : AbstractJacksonHttpMessageConverter<ObjectMapper>(objectMapper, MediaType("application", "javascript"))
