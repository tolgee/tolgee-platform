package io.tolgee.mcp

import io.tolgee.mcp.tools.McpToolRegistrar

interface McpToolsProvider {
  fun register(registrar: McpToolRegistrar)
}
