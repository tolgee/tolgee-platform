# MCP Server Publishing

Tolgee is listed in the [official MCP Registry](https://registry.modelcontextprotocol.io) as a remote server pointing at `https://app.tolgee.io/mcp/developer`.

## `server.json`

The registry entry is defined by [`server.json`](../../server.json) at the repo root. It contains the server name (`io.github.tolgee/tolgee`), description, version, and the remote URL + `X-API-Key` header spec. Full schema: https://static.modelcontextprotocol.io/schemas/2025-12-11/server.schema.json.

## Prerequisite: public org membership

`mcp-publisher login github` only grants access to `io.github.tolgee/*` if your membership in the [`tolgee` GitHub org](https://github.com/orgs/tolgee/people) is **public**. Change visibility to Public on that page before logging in, otherwise publish will fail with "You do not have permission to publish this server".

## Publishing a new version

1. Install `mcp-publisher` if you don't have it: `brew install mcp-publisher`
2. Bump `version` in [`server.json`](../../server.json).
3. Validate: `mcp-publisher validate`
4. Publish: `mcp-publisher login github && mcp-publisher publish`
