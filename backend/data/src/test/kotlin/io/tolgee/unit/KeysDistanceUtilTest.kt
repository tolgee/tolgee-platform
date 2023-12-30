package io.tolgee.unit

import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.dtos.queryResults.KeyIdFindResult
import io.tolgee.model.Project
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.bigMeta.KeysDistanceDto
import io.tolgee.service.bigMeta.KeysDistanceUtil
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class KeysDistanceUtilTest {
  private val relatedKeysRequest =
    mutableListOf(
      RelatedKeyDto(keyName = "key1", namespace = "a"),
      RelatedKeyDto(keyName = "key2", namespace = null),
      RelatedKeyDto(keyName = "key3", namespace = "a"),
    )

  private val project: Project = mock()
  private val bigMetaService: BigMetaService = mock()

  init {
    whenever(project.id).thenReturn(1)

    whenever(bigMetaService.findExistingKeyDistances(any(), any()))
      .thenReturn(
        setOf(
          KeysDistanceDto(1, 3, projectId = 0).also { keysDistance ->
            keysDistance.score = 10000
            keysDistance.hits = 10
          },
          KeysDistanceDto(3, 4, projectId = 0).also { keysDistance ->
            keysDistance.score = 10000
            keysDistance.hits = 1
          },
        ),
      )
    whenever(bigMetaService.getKeyIdsForItems(any(), any())).thenReturn(
      mutableListOf(
        KeyIdFindResult(
          id = 1,
          name = "key1",
          namespace = "a",
        ),
        KeyIdFindResult(
          id = 2,
          name = "key2",
          namespace = null,
        ),
        KeyIdFindResult(
          id = 3,
          name = "key3",
          namespace = "a",
        ),
        KeyIdFindResult(
          id = 4,
          name = "key4",
          namespace = "a",
        ),
      ),
    )
  }

  @Test
  fun `it works`() {
    val result =
      KeysDistanceUtil(relatedKeysRequest, project, bigMetaService)
        .newDistances

    result.assert.hasSize(4)
    result.singleOrNull { it.key1Id == 1L && it.key2Id == 2L }!!.score.assert.isEqualTo(10000)
    result.singleOrNull { it.key1Id == 2L && it.key2Id == 3L }!!.score.assert.isEqualTo(10000)
    val key1And3Distance = result.singleOrNull { it.key1Id == 1L && it.key2Id == 3L }!!
    key1And3Distance.score.assert.isEqualTo(9818L)
    key1And3Distance.hits.assert.isEqualTo(11)

    val key3And4Distance = result.singleOrNull { it.key1Id == 3L && it.key2Id == 4L }!!
    key3And4Distance.score.assert.isEqualTo(5000)
    key3And4Distance.hits.assert.isEqualTo(2)
  }
}
