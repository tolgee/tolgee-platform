package io.tolgee.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

@Configuration
@Lazy(false)
class OctetStreamSupportConfiguration(
  converter: MappingJackson2HttpMessageConverter,
) {
  init {
    val supportedMediaTypes = converter.supportedMediaTypes.toMutableList()
    supportedMediaTypes.add(MediaType("application", "octet-stream"))
    converter.supportedMediaTypes = supportedMediaTypes
  }
}
