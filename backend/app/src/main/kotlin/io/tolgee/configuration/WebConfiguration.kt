/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import io.tolgee.activity.ActivityHandlerInterceptor
import io.tolgee.component.TestClockHeaderFilter
import io.tolgee.component.VersionFilter
import io.tolgee.configuration.tolgee.TolgeeProperties
import jakarta.servlet.MultipartConfigElement
import org.springframework.boot.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.CacheControl
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.util.unit.DataSize
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

@Configuration
@EnableScheduling
// proxyTargetClass reason: Prefer CGLIB-proxies. Otherwise, we would need to include
// all necessary methods in interfaces every time we implement an interface in a component
// class when we need to use it with `@Autowired`.
@EnableAsync(proxyTargetClass = true)
class WebConfiguration(
  private val tolgeeProperties: TolgeeProperties,
  private val activityInterceptor: ActivityHandlerInterceptor,
) : WebMvcConfigurer {
  override fun addViewControllers(registry: ViewControllerRegistry) {
    registry.run {
      val forwardTo = "forward:/"
      addViewController("/{spring:[\\w-_=]+}")
        .setViewName(forwardTo)
      addViewController("/**/{spring:[\\w-_=]+}")
        .setViewName(forwardTo)
      addViewController("/{spring:\\w+}/**{spring:?!(\\.js|\\.css||\\.woff2)$}")
        .setViewName(forwardTo)
    }
  }

  override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
    registry
      .addResourceHandler("/**/*.js", "/**/*.woff2", "/**/*.css", "/**/*.svg")
      .addResourceLocations("classpath:/static/")
      .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
  }

  override fun addCorsMappings(registry: CorsRegistry) {
    registry
      .addMapping("/**")
      .allowedMethods("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
      .exposedHeaders(VersionFilter.TOLGEE_VERSION_HEADER_NAME, TestClockHeaderFilter.TOLGEE_TEST_CLOCK_HEADER_NAME)
  }

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(activityInterceptor)
  }

  @Bean
  fun secureRandom(): SecureRandom {
    return SecureRandom()
  }

  // Declared as JsonMapper (not ObjectMapper) so Boot's @ConditionalOnMissingBean(JsonMapper)
  // jacksonJsonMapper backs off, leaving this as the single primary mapper.
  @Bean
  @Primary
  fun objectMapper(): JsonMapper {
    return jacksonMapperBuilder()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
      .build()
  }

  @Bean
  fun multipartConfigElement(): MultipartConfigElement {
    val factory = MultipartConfigFactory()
    factory.setMaxFileSize(DataSize.ofKilobytes(tolgeeProperties.maxUploadFileSize.toLong()))
    factory.setMaxRequestSize(DataSize.ofKilobytes(tolgeeProperties.maxUploadFileSize.toLong()))
    return factory.createMultipartConfig()
  }
}
