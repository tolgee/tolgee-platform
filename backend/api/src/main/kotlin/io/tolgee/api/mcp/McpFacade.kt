package io.tolgee.api.mcp

/**
 * Marks a facade shared by one REST controller and its MCP twin tool.
 *
 * The MCP endpoint is a RouterFunction: no HandlerInterceptor, no `@Valid`, no
 * `@ControllerAdvice` runs for it. Logic written in a controller body is therefore
 * invisible to the MCP tool (GHSA-j998-54vh-rwpg). A facade holds the whole endpoint
 * body minus the HTTP layer, so both sides run the same code:
 *
 * - The REST controller method shrinks to: facade call + response assembler.
 * - The MCP tool calls the facade inside `McpRequestContext.executeAs`.
 * - The facade validates its request DTOs FIRST, via [checkValid]. Controllers keep
 *   `@Valid`, so on REST this validation is unreachable and changes nothing; on MCP
 *   it is the only validation that runs.
 *
 * MCP tools may inject only [McpFacade] implementations besides infrastructure —
 * McpToolDependenciesTest enforces this.
 */
interface McpFacade
