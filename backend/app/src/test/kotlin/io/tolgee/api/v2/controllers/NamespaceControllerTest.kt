package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class NamespaceControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: NamespacesTestData

  @BeforeEach
  fun createData() {
    testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns namespaces`() {
    performProjectAuthGet("namespaces").andIsOk.andAssertThatJson {
      node("_embedded.namespaces") {
        isArray.hasSize(2)
        node("[0]") {
          node("id").isValidId
          node("name").isEqualTo("ns-1")
        }
        node("[1]") {
          node("id").isValidId
          node("name").isEqualTo("ns-2")
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns used namespaces`() {
    performProjectAuthGet("used-namespaces").andIsOk.andAssertThatJson {
      node("_embedded.namespaces") {
        isArray.hasSize(3)
      }
    }
    projectSupplier = { testData.defaultUnusedProject }
    performProjectAuthGet("used-namespaces").andIsOk.andAssertThatJson {
      node("_embedded.namespaces") {
        isArray.hasSize(1)
        node("[0].name").isEqualTo("ns-1")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates namespace name`() {
    performProjectAuthPut(
      "namespaces/${testData.namespaces[getNs1Def()]?.id}",
      mapOf("name" to "ns-new"),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("ns-new")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot rename to existing`() {
    performProjectAuthPut(
      "namespaces/${testData.namespaces[getNs1Def()]?.id}",
      mapOf("name" to "ns-2"),
    ).andAssertError.hasCode("namespace_exists")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `validates rename`() {
    val nsId = testData.namespaces[getNs1Def()]?.id
    performProjectAuthPut(
      "namespaces/$nsId",
      mapOf("name" to ""),
    ).andAssertError.isStandardValidation.onField("name")
    performProjectAuthPut(
      "namespaces/$nsId",
      mapOf("name" to null),
    ).andAssertError.isStandardValidation.onField("name")
    performProjectAuthPut(
      "namespaces/$nsId",
      mapOf("name" to "  "),
    ).andAssertError.isStandardValidation.onField("name")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `namespace by name`() {
    projectSupplier = { testData.dotProject }
    val ns = testData.namespaces[testData.dotProject to "ns.1"]
    performProjectAuthGet("namespace-by-name/${ns?.name}")
      .andIsOk
      .andAssertThatJson {
        node("id").isEqualTo(ns?.id)
      }
  }

  private fun getNs1Def() = testData.projectBuilder.self to "ns-1"
}
