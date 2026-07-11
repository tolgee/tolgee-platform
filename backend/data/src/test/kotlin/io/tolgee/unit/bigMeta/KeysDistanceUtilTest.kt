package io.tolgee.unit.bigMeta

import io.tolgee.model.Project
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.bigMeta.KeysDistanceDto
import io.tolgee.service.bigMeta.KeysDistanceUtil
import io.tolgee.testing.assert
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class KeysDistanceUtilTest {
  private val project: Project = mock()
  private val bigMetaService: BigMetaService = mock()

  lateinit var testData: KeysDistanceUnitTestData

  @BeforeEach
  fun setup() {
    testData = KeysDistanceUnitTestData()
  }

  private fun initMocks(data: KeysDistanceUnitTestData) {
    whenever(project.id).thenReturn(1)

    whenever(bigMetaService.findExistingKeyDistances(any(), any()))
      .thenReturn(
        data.existingDistances,
      )
    whenever(bigMetaService.getKeyIdsForItems(any(), any())).thenReturn(
      data.existingKeys,
    )
  }

  @Test
  fun `it works (simple case)`() {
    testData.createRequestData(3, "a")
    testData.generateExistingDistances(2..4)
    val (toStore, toDelete) = getResult()
    toStore.assert.hasSize(5)
    toDelete.assert.hasSize(0)
    // these are the new elements, not included in existing distances
    toStore.assertDistance(0, 1).distanceEqualsTo(0.0).hitsEqualsTo(1)
    toStore.assertDistance(0, 2).distanceEqualsTo(1.0).hitsEqualsTo(1)
    toStore.assertDistance(1, 2).distanceEqualsTo(0.0).hitsEqualsTo(1)

    // these are the elements with increased distance
    // for these elements, new distance is computed as average of the existing distance and the new distance
    // the new distances is virtually pushing the elements out of the focus window and so such elements are more probable
    // candidates for deletion
    toStore.assertDistance(2, 3).distanceEqualsTo(10.0).hitsEqualsTo(2)
    toStore.assertDistance(2, 4).distanceEqualsTo(10.5).hitsEqualsTo(2)
  }

  @Test
  fun `it deletes distances`() {
    testData.createRequestData(2, "a")
    testData.generateExistingDistances(1..30)
    val (toStore, toDelete) = getResult()
    toStore.forKeyId(1).assert.hasSize(20)
    toDelete.forKeyId(1).assert.hasSize(10)
  }

  private fun MutableSet<KeysDistanceDto>.forKeyId(keyId: Long): List<KeysDistanceDto> {
    return this.filter { it.key1Id == keyId || it.key2Id == keyId }
  }

  private fun getResult(): Pair<MutableSet<KeysDistanceDto>, MutableSet<KeysDistanceDto>> {
    initMocks(testData)
    return KeysDistanceUtil(testData.requestData, project, bigMetaService).toStoreAndDelete
  }

  private fun MutableSet<KeysDistanceDto>.assertDistance(
    key1Id: Long,
    key2Id: Long,
  ): ObjectAssert<KeysDistanceDto> {
    return this.find { it.key1Id == key1Id && it.key2Id == key2Id }?.assert
      ?: throw AssertionError("Distance not found")
  }

  private fun ObjectAssert<KeysDistanceDto>.hitsEqualsTo(hits: Long): ObjectAssert<KeysDistanceDto> {
    this.extracting { it.hits }.isEqualTo(hits)
    return this
  }

  private fun ObjectAssert<KeysDistanceDto>.distanceEqualsTo(distance: Double): ObjectAssert<KeysDistanceDto> {
    this.extracting { it.distance }.isEqualTo(distance)
    return this
  }
}
