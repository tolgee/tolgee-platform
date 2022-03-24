package io.tolgee.commandLineRunners

import com.corundumstudio.socketio.SocketIOServer
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class SocketIoServerCommandLineRunner(
  private val server: SocketIOServer? = null,
  private val tolgeeProperties: TolgeeProperties
) :
  CommandLineRunner, ApplicationListener<ContextClosedEvent> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun run(vararg args: String) {
    if (tolgeeProperties.socketIo.enabled) {
      server?.start()
    }
  }

  override fun onApplicationEvent(event: ContextClosedEvent?) {
    try {
      server?.stop()
    } catch (e: NullPointerException) {
      logger.info("Netty SocketIO thrown Null Pointer Exception.")
    }
  }
}
