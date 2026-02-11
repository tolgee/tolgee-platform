package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.repository.KeysDistanceRepository
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class McpBigMetaToolsTest : AbstractMcpTest() {
  @Autowired
  lateinit var keysDistanceRepository: KeysDistanceRepository

  lateinit var data: McpTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClient(data.pat.token!!)
  }

  @Test
  fun `store_big_meta stores key relationships`() {
    callTool(
      client,
      "create_key",
      mapOf("projectId" to data.projectId, "keyName" to "key.one"),
    )
    callTool(
      client,
      "create_key",
      mapOf("projectId" to data.projectId, "keyName" to "key.two"),
    )

    val json =
      callToolAndGetJson(
        client,
        "store_big_meta",
        mapOf(
          "projectId" to data.projectId,
          "relatedKeysInOrder" to
            listOf(
              mapOf("keyName" to "key.one"),
              mapOf("keyName" to "key.two"),
            ),
        ),
      )
    assertThat(json["stored"].asBoolean()).isTrue()
    assertThat(json["keyCount"].asInt()).isEqualTo(2)

    val key1 = keyService.find(data.projectId, "key.one", null)!!
    waitForNotThrowing(timeout = 5000, pollTime = 100) {
      val closeKeys = keysDistanceRepository.getCloseKeys(key1.id)
      assertThat(closeKeys).isNotEmpty()
    }
  }
}
