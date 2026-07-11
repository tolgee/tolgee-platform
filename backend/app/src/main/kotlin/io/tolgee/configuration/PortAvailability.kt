package io.tolgee.configuration

import okhttp3.internal.closeQuietly
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.net.ServerSocket
import kotlin.system.exitProcess

@Configuration
class PortAvailability {
  private val logger = LoggerFactory.getLogger(PortAvailability::class.java)

  @Bean
  fun portAvailabilityChecker(
    @Value("\${server.port:8080}") port: Int,
    @Value("\${management.server.port:#{null}}") managementPort: Int?,
  ): ServletContextInitializer {
    return ServletContextInitializer {
      checkPortAvailability(port, "server.port")
      managementPort?.also { checkPortAvailability(it, "management.server.port") }
    }
  }

  private fun checkPortAvailability(
    port: Int,
    propertyKey: String,
  ) {
    try {
      ServerSocket(port).closeQuietly()
    } catch (e: IOException) {
      logger.error(
        "Port $port is already in use. " +
          "Please free the port or specify a different port using the `$propertyKey` property.",
        e,
      )
      exitProcess(1)
    }
  }
}
