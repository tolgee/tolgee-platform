package io.tolgee.mcp

import io.modelcontextprotocol.server.McpSyncServer

interface McpToolsProvider {
  fun register(server: McpSyncServer)
}
