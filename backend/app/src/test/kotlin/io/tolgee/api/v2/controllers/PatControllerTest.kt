package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.PatTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import java.math.BigDecimal
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class PatControllerTest : AuthorizedControllerTest() {
  lateinit var testData: PatTestData

  @BeforeEach
  fun createData() {
    testData = PatTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `get all works`() {
    performAuthGet("/v2/pats").andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.pats") {
        node("[0]") {
          node("description").isEqualTo("Expired PAT")
          node("lastUsedAt").isEqualTo(1661342385000)
          node("expiresAt").isEqualTo(1661342685000)
          node("id").isValidId
        }
      }
    }
  }

  @Test
  fun `get one works`() {
    performAuthGet("/v2/pats/${testData.expiredPat.id}").andIsOk.andAssertThatJson {
      node("description").isEqualTo("Expired PAT")
      node("lastUsedAt").isEqualTo(1661342385000)
      node("expiresAt").isEqualTo(1661342685000)
      node("id").isValidId
    }
  }

  @Test
  fun `create works`() {
    val description = "I am new!"
    val expiresAt = Date().time + 1000
    performAuthPost(
      "/v2/pats",
      mapOf(
        "description" to description,
        "expiresAt" to expiresAt,
      ),
    ).andIsCreated.andAssertThatJson {
      node("description").isEqualTo(description)
      node("expiresAt").isEqualTo(expiresAt)
      node("token").isString.hasSizeGreaterThan(10)
      node("token").isString.startsWith("tgpat_")
    }
  }

  @Test
  fun `delete works`() {
    performAuthDelete(
      "/v2/pats/${testData.expiredPat.id}",
    ).andIsOk

    assertThat(patService.find(testData.expiredPat.id)).isNull()
  }

  @Test
  fun `regenerate works`() {
    val oldToken = testData.expiredPat.tokenHash
    val expiresAt = Date().time + 10000
    performAuthPut(
      "/v2/pats/${testData.expiredPat.id}/regenerate",
      mapOf(
        "expiresAt" to expiresAt,
      ),
    ).andIsOk.andAssertThatJson {
      node("token").isString.startsWith("tgpat_")
      node("expiresAt").isEqualTo(expiresAt)
    }

    patService
      .get(testData.expiredPat.id)
      .tokenHash.assert
      .isNotEqualTo(oldToken)
  }

  @Test
  fun `regenerate works (never expires)`() {
    val oldToken = testData.pat.tokenHash
    testData.pat.expiresAt.assert
      .isNull()

    performAuthPut(
      "/v2/pats/${testData.pat.id}/regenerate",
      mapOf(
        "expiresAt" to null,
      ),
    ).andIsOk.andAssertThatJson {
      node("token").isString.startsWith("tgpat_")
      node("expiresAt").isEqualTo(null)
    }

    patService
      .get(testData.pat.id)
      .tokenHash.assert
      .isNotEqualTo(oldToken)
  }

  @Test
  fun `update works`() {
    val description = "I am updated!"
    performAuthPut(
      "/v2/pats/${testData.expiredPat.id}",
      mapOf(
        "description" to description,
      ),
    ).andIsOk.andAssertThatJson {
      node("description").isString.isEqualTo(description)
    }

    patService
      .get(testData.expiredPat.id)
      .description.assert
      .isEqualTo(description)
  }

  @Test
  fun `returns current PAT information`() {
    val headers = HttpHeaders()
    headers["x-api-key"] = "tgpat_${testData.pat.token!!}"

    performGet("/v2/pats/current", headers).andPrettyPrint.andAssertThatJson {
      node("id").isValidId.isEqualTo(BigDecimal(testData.pat.id))
      node("user.id").asNumber().isEqualTo(BigDecimal(testData.user.id))
    }
  }

  @Test
  fun `returns 400 on get current PAT info with Bearer auth`() {
    loginAsUser(testData.user)
    performAuthGet("/v2/pats/current").andIsBadRequest
  }
}
