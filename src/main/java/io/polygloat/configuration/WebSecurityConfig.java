package io.polygloat.configuration;

import io.polygloat.configuration.polygloat.PolygloatProperties;
import io.polygloat.security.InternalDenyFilter;
import io.polygloat.security.JwtTokenFilter;
import io.polygloat.security.api_key_auth.ApiAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtTokenFilter jwtTokenFilter;
    private final PolygloatProperties configuration;
    private final ApiAuthFilter apiAuthFilter;
    private final InternalDenyFilter internalDenyFilter;

    @Autowired
    public WebSecurityConfig(JwtTokenFilter jwtTokenFilter, PolygloatProperties configuration, ApiAuthFilter apiAuthFilter, InternalDenyFilter internalDenyFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.configuration = configuration;
        this.apiAuthFilter = apiAuthFilter;
        this.internalDenyFilter = internalDenyFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (configuration.getAuthentication().getEnabled()) {
            http
                    .csrf().disable().cors().and()
                    //if jwt token is provided in header, this filter will manualy authorize user, so the request is not gonna reach the ldap auth
                    .addFilterBefore(internalDenyFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(apiAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    //this is used to authorize user's app calls with genrated api key
                    .authorizeRequests()
                    .antMatchers("/api/public/**", "/webjars/**", "/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs").permitAll()
                    .antMatchers("/api/**", "/uaa", "/uaa/**").authenticated()
                    .and().sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();
            return;
        }

        http
                .csrf().disable()
                .cors().and()
                .authorizeRequests().anyRequest().permitAll();
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        var ldapConfiguration = configuration.getAuthentication().getLdap();

        if (ldapConfiguration.getEnabled()) {
            auth
                    .ldapAuthentication()
                    .contextSource()
                    .url(ldapConfiguration.getUrls() + ldapConfiguration.getBaseDn())
                    .managerDn(ldapConfiguration.getSecurityPrincipal())
                    .managerPassword(ldapConfiguration.getPrincipalPassword())
                    .and()
                    .userDnPatterns(ldapConfiguration.getUserDnPattern());
            return;
        }
    }

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
