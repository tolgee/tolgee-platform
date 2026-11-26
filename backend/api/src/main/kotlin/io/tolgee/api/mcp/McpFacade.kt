package io.tolgee.api.mcp

/**
 * The MCP endpoint is a RouterFunction: no HandlerInterceptor, no `@Valid`, no
 * `@ControllerAdvice` runs for it, so logic written only in a controller body never
 * runs for the MCP tool (GHSA-j998-54vh-rwpg). Rules for a facade method:
 *
 * - Validate request DTOs FIRST, via [checkValid].
 * - The MCP tool calls the facade inside `McpRequestContext.executeAs`.
 *
 * Most facades are shared with a REST controller; the MCP-only exceptions are listed
 * in McpToolDependenciesTest.mcpOnlyFacades.
 */
interface McpFacade
