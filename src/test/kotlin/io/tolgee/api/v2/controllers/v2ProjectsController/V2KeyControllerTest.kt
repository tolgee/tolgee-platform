package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.annotations.ProjectJWTAuthTestMethod
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.dtos.request.CreateKeyDto
import io.tolgee.dtos.request.EditKeyDto
import io.tolgee.fixtures.*
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class V2KeyControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  companion object {
    const val LONGER_NAME = "FrWvKivzSEqhTeyJwLvlHJnMRWsqwwto0Vxfxd45OMQXkWLnmMB2SSW" +
      "v0azV5BOb8uPf1XgvZOLtbLJuAHnzgG1lmiGMVY4FKrL8p1wQlZQg" +
      "0BGLDG0bRD4WSVneChpPTbwN5bUWLa8ItXSXwP9nbE0GJi6ezwkS" +
      "McWs3Mcr7W6l20DLGQIfAVALAuPXICRxshLbq57GV"
  }

  lateinit var testData: KeysTestData

  @BeforeMethod
  fun setup() {
    testData = KeysTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key"))
      .andIsCreated.andPrettyPrint.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("super_key")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with translations and tags`() {
    val keyName = "super_key"
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        tags = listOf("tag", "tag2")
      )
    )
      .andIsCreated.andPrettyPrint.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo(keyName)
      }
    assertThat(tagService.find(project, "tag")).isNotNull
    val key = keyService.get(project.id, keyName).orElseThrow()
    assertThat(translationService.find(key, testData.english).get().text).isEqualTo("EN")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when not valid`() {
    performProjectAuthPost("keys", CreateKeyDto(name = ""))
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("STANDARD_VALIDATION") {
          node("name").isString
        }
      }

    performProjectAuthPost("keys", CreateKeyDto(name = LONGER_NAME))
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("STANDARD_VALIDATION") {
          node("name").isEqualTo("length must be between 1 and 200")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when key exists`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "first_key"))
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("code").isEqualTo("key_exists")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates key name`() {
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = "test"))
      .andIsOk.andPrettyPrint.andAssertThatJson {
        node("name").isEqualTo("test")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update if invalid`() {
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = ""))
      .andIsBadRequest
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = LONGER_NAME))
      .andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update if from other project`() {
    projectSupplier = { testData.project2 }
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = "aasda"))
      .andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes single key`() {
    performProjectAuthDelete("keys/${testData.firstKey.id}", null).andIsOk
    assertThat(keyService.get(testData.firstKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes single key with references`() {
    performProjectAuthDelete("keys/${testData.keyWithReferences.id}", null).andIsOk
    assertThat(keyService.get(testData.keyWithReferences.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys with references`() {
    performProjectAuthDelete("keys/${testData.keyWithReferences.id},${testData.keyWithReferences.id}", null).andIsOk
    assertThat(keyService.get(testData.keyWithReferences.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not delete if not in project`() {
    projectSupplier = { testData.project2 }
    performProjectAuthDelete("keys/${testData.firstKey.id}", null)
      .andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys`() {
    projectSupplier = { testData.project }
    performProjectAuthDelete("keys/${testData.firstKey.id},${testData.secondKey.id}", null).andIsOk
    assertThat(keyService.get(testData.firstKey.id)).isEmpty
    assertThat(keyService.get(testData.secondKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not delete multiple if not in project`() {
    projectSupplier = { testData.project2 }
    performProjectAuthDelete("keys/${testData.secondKey.id},${testData.firstKey.id}", null)
      .andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }
}
