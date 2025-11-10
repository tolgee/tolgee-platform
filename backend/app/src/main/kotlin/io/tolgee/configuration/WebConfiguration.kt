/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.activity.ActivityHandlerInterceptor
import io.tolgee.component.TestClockHeaderFilter
import io.tolgee.component.VersionFilter
import io.tolgee.configuration.tolgee.TolgeeProperties
import jakarta.servlet.MultipartConfigElement
import org.springframework.boot.web.servlet.MultipartConfigFactory
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
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

@Configuration
@EnableScheduling
@EnableAsync
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

  @Bean
  @Primary
  fun objectMapper(): ObjectMapper {
    return jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  @Bean
  fun multipartConfigElement(): MultipartConfigElement {
    val factory = MultipartConfigFactory()
    factory.setMaxFileSize(DataSize.ofKilobytes(tolgeeProperties.maxUploadFileSize.toLong()))
    factory.setMaxRequestSize(DataSize.ofKilobytes(tolgeeProperties.maxUploadFileSize.toLong()))
    return factory.createMultipartConfig()
  }
}
