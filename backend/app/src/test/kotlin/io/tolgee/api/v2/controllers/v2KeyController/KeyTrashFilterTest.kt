package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeyTrashTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

/**
 * Tests for the trashed-keys query path: `GET /v2/projects/{id}/keys/trash`.
 *
 * This endpoint reuses TranslationViewDataProvider with `params.trashed = true`. It exercises the
 * code paths in `QueryBase.init` (deletedAt branch), `addDeletedAtSelection`, `addDeletedBySelection`,
 * and `QueryGlobalFiltering.filterDeletedByUserId`. These were previously almost completely untested.
 */
@SpringBootTest
@AutoConfigureMockMvc
class KeyTrashFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeyTrashTestData

  @BeforeEach
  fun setup() {
    testData = KeyTrashTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filterDeletedByUserId returns only keys deleted by the specified user`() {
    val key01 = testData.numberedKeys[0]
    val key02 = testData.numberedKeys[1]

    keyService.softDeleteMultiple(listOf(key01.id), testData.user)
    keyService.softDeleteMultiple(listOf(key02.id), testData.secondUser)

    performProjectAuthGet("/keys/trash?filterDeletedByUserId=${testData.user.id}")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].name").isEqualTo("key 01")
        }
        node("page.totalElements").isEqualTo(1)
      }

    performProjectAuthGet("/keys/trash?filterDeletedByUserId=${testData.secondUser.id}")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].name").isEqualTo("key 02")
        }
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filterDeletedByUserId accepts multiple user ids`() {
    val key01 = testData.numberedKeys[0]
    val key02 = testData.numberedKeys[1]
    val key03 = testData.numberedKeys[2]

    keyService.softDeleteMultiple(listOf(key01.id), testData.user)
    keyService.softDeleteMultiple(listOf(key02.id), testData.secondUser)
    // key03 stays active — not deleted at all

    performProjectAuthGet(
      "/keys/trash?filterDeletedByUserId=${testData.user.id}&filterDeletedByUserId=${testData.secondUser.id}",
    ).andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
      }

    // Sanity check: active key did not leak into trashed listing
    keyService
      .findOptional(key03.id)
      .get()
      .deletedAt
      .let { assert(it == null) }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `trashed listing combined with filterTag returns only trashed keys with that tag`() {
    keyService.softDeleteMultiple(listOf(testData.keyWithTag.id, testData.numberedKeys[0].id), testData.user)

    performProjectAuthGet("/keys/trash?filterTag=Cool tag")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].name").isEqualTo("Key with tag")
        }
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `trashed listing combined with filterKeyName returns only trashed matching keys`() {
    val key05 = testData.numberedKeys[4]
    val key06 = testData.numberedKeys[5]
    keyService.softDeleteMultiple(listOf(key05.id, key06.id), testData.user)

    performProjectAuthGet("/keys/trash?filterKeyName=key 05")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].name").isEqualTo("key 05")
        }
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `trashed listing combined with search filter`() {
    val key03 = testData.numberedKeys[2]
    val key04 = testData.numberedKeys[3]
    keyService.softDeleteMultiple(listOf(key03.id, key04.id, testData.keyWithTag.id), testData.user)

    // search matches translations text "I am key 03's german translation."
    performProjectAuthGet("/keys/trash?search=key 03")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        // The search hits both key name "key 03" and any text containing "key 03"
        node("page.totalElements").isEqualTo(1)
        node("_embedded.keys[0].name").isEqualTo("key 03")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `trashed listing returns deletedAt and deletedBy info`() {
    val key01 = testData.numberedKeys[0]
    keyService.softDeleteMultiple(listOf(key01.id), testData.user)

    performProjectAuthGet("/keys/trash")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0]") {
          node("name").isEqualTo("key 01")
          node("deletedAt").isPresent()
          node("deletedBy.id").isEqualTo(testData.user.id)
          node("deletedBy.username").isEqualTo("franta")
        }
      }
  }
}
