package io.tolgee.configuration

import io.tolgee.component.ExceptionHandlerFilter
import io.tolgee.component.TestClockHeaderFilter
import io.tolgee.component.VersionFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FiltersConfiguration {
  @Bean("filterRegistrationVersion")
  fun versionFilter(versionFilter: VersionFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(versionFilter)
    // Just before Spring Security's filter chain (its default order is -100;
    // Boot 4 removed the SecurityProperties.DEFAULT_FILTER_ORDER constant).
    registration.order = -101
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
