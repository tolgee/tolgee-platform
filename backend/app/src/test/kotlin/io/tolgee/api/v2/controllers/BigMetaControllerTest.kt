package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BigMetaTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.service.BigMetaService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BigMetaControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: BigMetaTestData

  @Autowired
  lateinit var bigMetaService: BigMetaService

  @BeforeEach
  fun setup() {
    testData = BigMetaTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
    userAccount = testData.userAccount
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it stores`() {
    performProjectAuthPost(
      "big-meta",
      mapOf(
        "items" to listOf(
          mapOf(
            "namespace" to "hehe",
            "keyName" to "haha",
            "location" to "hoho",
            "type" to "SCRAPE",
            "contextData" to mapOf("huhu" to "haha")
          ),
        )
      )
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("[0]") {
        node("id").satisfies {
          bigMetaService.find(it.toString().toLong()).assert.isNotNull
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns by id`() {
    performProjectAuthGet("big-meta/${testData.someBigMeta.id}").andIsOk.andAssertThatJson {
      node("id").isEqualTo(testData.someBigMeta.id)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it lists for key`() {
    performProjectAuthGet("keys/${testData.yepKey.id}/big-meta").andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.bigMeta") {
        isArray.hasSize(3)
        node("[0]") {
          node("id").isEqualTo(testData.someBigMeta.id)
          node("contextData").isEqualTo(
            """
            {
              "random data a" : "haha"
            }
            """.trimIndent()
          )
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it lists all`() {
    performProjectAuthGet("big-meta").andIsOk.andAssertThatJson {
      node("_embedded.bigMeta") {
        isArray.hasSize(4)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it deletes`() {
    performProjectAuthDelete("big-meta/${testData.someBigMeta.id}").andIsOk

    bigMetaService.find(testData.someBigMeta.id).assert.isNull()
  }
}
