package io.tolgee

import com.corundumstudio.socketio.SocketIOServer
import io.tolgee.configuration.tolgee.TolgeeProperties
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
  override fun run(vararg args: String) {
    if (tolgeeProperties.socketIo.enabled) {
      server?.start()
    }
  }

  override fun onApplicationEvent(event: ContextClosedEvent?) {
    server?.stop()
  }
}
