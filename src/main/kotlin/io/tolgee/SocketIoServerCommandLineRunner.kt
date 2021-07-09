package io.tolgee

import com.corundumstudio.socketio.SocketIOServer
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component


@Component
class SocketIoServerCommandLineRunner(val server: SocketIOServer?) :
  CommandLineRunner, ApplicationListener<ContextClosedEvent> {
  override fun run(vararg args: String) {
    server?.start()
  }

  override fun onApplicationEvent(event: ContextClosedEvent?) {
    server?.stop()
  }
}
