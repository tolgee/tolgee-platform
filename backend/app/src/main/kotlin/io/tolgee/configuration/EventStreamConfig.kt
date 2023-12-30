package io.tolgee.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class EventStreamConfig(
  private val objectMapper: ObjectMapper,
) : WebMvcConfigurer {
  override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>?>) {
    converters.add(EventStreamHttpMessageConverter(objectMapper))
    converters.add(JavascriptHttpMessageConverter(objectMapper))
  }
}

class EventStreamHttpMessageConverter(
  objectMapper: ObjectMapper,
) : AbstractJackson2HttpMessageConverter(objectMapper, MediaType.TEXT_EVENT_STREAM)

class JavascriptHttpMessageConverter(
  objectMapper: ObjectMapper,
) : AbstractJackson2HttpMessageConverter(objectMapper, MediaType("application", "javascript"))
