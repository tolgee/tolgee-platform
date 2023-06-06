package io.tolgee.configuration

import io.tolgee.component.VersionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfiguration {
  @Bean
  fun corsMappingConfigurer(): WebMvcConfigurer {
    return object : WebMvcConfigurer {
      override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
          .exposedHeaders(VersionFilter.TOLGEE_VERSION_HEADER_NAME)
      }
    }
  }
}
