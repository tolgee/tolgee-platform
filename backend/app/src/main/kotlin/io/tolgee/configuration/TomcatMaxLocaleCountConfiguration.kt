package io.tolgee.configuration

import org.springframework.boot.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TomcatMaxLocaleCountConfiguration {
  /**
   * Tomcat 11.0.27 caps Accept-Language parsing at 10 locales (maxLocaleCount) and rejects the
   * whole request above that, so browsers with more than 10 configured languages get a 500 —
   * common among users of a translation tool. Tomcat is set to ignore extra locales instead
   * (planned for 11.0.28), but a low cap would still drop locales such users send.
   */
  @Bean
  fun tomcatMaxLocaleCountCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> =
    WebServerFactoryCustomizer { factory ->
      factory.addConnectorCustomizers(
        TomcatConnectorCustomizer { connector -> connector.maxLocaleCount = 50 },
      )
    }
}
