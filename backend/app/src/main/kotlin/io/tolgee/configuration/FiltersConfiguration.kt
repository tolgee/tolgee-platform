package io.tolgee.configuration

import io.tolgee.component.ExceptionHandlerFilter
import io.tolgee.component.TestClockHeaderFilter
import io.tolgee.component.VersionFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FiltersConfiguration {
  companion object {
    private const val JUST_BEFORE_SECURITY_FILTER_ORDER = -101
  }

  @Bean("filterRegistrationVersion")
  fun versionFilter(versionFilter: VersionFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(versionFilter)
    registration.order = JUST_BEFORE_SECURITY_FILTER_ORDER
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
