package io.tolgee.socketio

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIONamespace
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import io.tolgee.model.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TranslationsConnectionListener(
  server: SocketIOServer,
  private val socketIoProjectProvider: SocketIoProjectProvider,
) {
  private var namespace: SocketIONamespace = server.addNamespace("/translations")
  private val log: Logger = LoggerFactory.getLogger(TranslationsConnectionListener::class.java)

  fun listen() {
    this.namespace.addConnectListener(onConnected)
  }

  private val onConnected: ConnectListener
    get() = ConnectListener { client: SocketIOClient ->
      val handshakeData = client.handshakeData
      try {
        val project = socketIoProjectProvider.getProject(handshakeData)
        val roomName = project.getRoomName()
        client.joinRoom(roomName)
        namespace.getRoomOperations(roomName).sendEvent("client_connected")
      } catch (e: Exception) {
        client.sendEvent("authorization_error")
        client.disconnect()
      }
      log.debug("Client[{}] - Connected to chat module through '{}'", client.sessionId.toString(), handshakeData.url)
    }

  private fun Project.getRoomName() = "translations-${this.id}"
}
