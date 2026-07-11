package io.tolgee.configuration

import io.tolgee.component.ExceptionHandlerFilter
import io.tolgee.component.TestClockHeaderFilter
import io.tolgee.component.VersionFilter
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FiltersConfiguration {
  @Bean("filterRegistrationVersion")
  fun versionFilter(versionFilter: VersionFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(versionFilter)
    registration.order = SecurityProperties.DEFAULT_FILTER_ORDER - 1
    return registration
  }

  @Bean("filterRegistrationTestClockHeader")
  fun testClockHeaderFilter(testClockHeaderFilter: TestClockHeaderFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(testClockHeaderFilter)
    return registration
  }

  @Bean("filterRegistrationSecurityExceptionHandler")
  fun exceptionHandlerFilter(exceptionHandlerFilter: ExceptionHandlerFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(exceptionHandlerFilter)
    registration.isEnabled = false
    return registration
  }
}
