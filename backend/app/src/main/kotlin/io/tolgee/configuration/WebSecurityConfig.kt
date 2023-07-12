package io.tolgee.configuration

import io.tolgee.activity.ActivityFilter
import io.tolgee.component.VersionFilter
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.security.DisabledAuthenticationFilter
import io.tolgee.security.InternalDenyFilter
import io.tolgee.security.JwtTokenFilter
import io.tolgee.security.ServerAdminFilter
import io.tolgee.security.apiKeyAuth.ApiKeyAuthFilter
import io.tolgee.security.patAuth.PatAuthFilter
import io.tolgee.security.project_auth.ProjectPermissionFilter
import io.tolgee.security.rateLimis.RateLimitLifeCyclePoint
import io.tolgee.security.rateLimits.RateLimitsFilterFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.BeanIds
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class WebSecurityConfig @Autowired constructor(
  private val jwtTokenFilter: JwtTokenFilter,
  private val configuration: TolgeeProperties,
  private val apiKeyAuthFilter: ApiKeyAuthFilter,
  private val internalDenyFilter: InternalDenyFilter,
  private val projectPermissionFilter: ProjectPermissionFilter,
  private val activityFilter: ActivityFilter,
  private val disabledAuthenticationFilter: DisabledAuthenticationFilter,
  private val rateLimitsFilterFactory: RateLimitsFilterFactory,
  private val patAuthFilter: PatAuthFilter,
  private val versionFilter: VersionFilter,
  private val serverAdminFilter: ServerAdminFilter
) : WebSecurityConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http
      .csrf()
      .disable()
      .cors()
      .and()
      .headers()
      .frameOptions().sameOrigin().and()
      .addFilterBefore(versionFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(internalDenyFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(rateLimitsFilterFactory.create(RateLimitLifeCyclePoint.ENTRY), InternalDenyFilter::class.java)
      // if jwt token is provided in header, this filter will authorize user, so the request is not gonna reach the ldap auth
      .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(patAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
      // this is used to authorize user's app calls with generated api key
      .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterAfter(disabledAuthenticationFilter, ApiKeyAuthFilter::class.java)
      .addFilterAfter(serverAdminFilter, DisabledAuthenticationFilter::class.java)
      .addFilterAfter(projectPermissionFilter, JwtTokenFilter::class.java)
      .addFilterAfter(activityFilter, ProjectPermissionFilter::class.java)
      .addFilterAfter(
        rateLimitsFilterFactory.create(RateLimitLifeCyclePoint.AFTER_AUTHORIZATION),
        ProjectPermissionFilter::class.java
      )
      .authorizeRequests()
      .antMatchers(
        "/api/public/**",
        "/webjars/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
        "/v2/api-docs",
        "/v2/public/**",
      ).permitAll()
      .antMatchers("/api/**", "/uaa", "/uaa/**", "/v2/**").authenticated()
      .and().sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
    return
  }

  override fun configure(auth: AuthenticationManagerBuilder) {
    val ldapConfiguration = configuration.authentication.ldap
    if (ldapConfiguration.enabled) {
      auth
        .ldapAuthentication()
        .contextSource()
        .url(ldapConfiguration.urls + ldapConfiguration.baseDn)
        .managerDn(ldapConfiguration.securityPrincipal)
        .managerPassword(ldapConfiguration.principalPassword)
        .and()
        .userDnPatterns(ldapConfiguration.userDnPattern)
      return
    }
  }

  @Bean(BeanIds.AUTHENTICATION_MANAGER)
  override fun authenticationManagerBean(): AuthenticationManager {
    return super.authenticationManagerBean()
  }
}
