package io.tolgee.mcp

import io.modelcontextprotocol.server.McpStatelessSyncServer

interface McpToolsProvider {
  fun register(server: McpStatelessSyncServer)
}
