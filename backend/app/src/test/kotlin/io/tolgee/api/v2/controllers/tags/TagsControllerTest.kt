package io.tolgee.api.v2.controllers.tags

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.dtos.request.key.TagKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.key.Key
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TagsControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TagsTestData

  @BeforeEach
  fun setup() {
    testData = TagsTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project tags are returned correctly`() {
    performProjectAuthGet("tags").andAssertThatJson {
      node("_embedded.tags") {
        isArray.hasSize(20)
        node("[0].id").isValidId
        node("[0].name").isEqualTo("existing tag")
      }
      node("page.totalElements").isNumber.isGreaterThan(BigDecimal(390))
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project tags are returned in order`() {
    performProjectAuthGet("tags?page=5").andAssertThatJson {
      node("_embedded.tags") {
        isArray.hasSize(20)
        node("[0].name").isEqualTo("tag 14 12")
        node("[19].name").isEqualTo("tag 15 10")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project tags paginate`() {
    performProjectAuthGet("tags?page=2").andPrettyPrint.andAssertThatJson {
      node("_embedded.tags") {
        isArray.hasSize(20)
        node("[0].id").isValidId
        node("[0].name").isEqualTo("tag 11 3")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project tags searches`() {
    performProjectAuthGet("tags?search=test").andPrettyPrint.andAssertThatJson {
      node("_embedded.tags") {
        isArray.hasSize(1)
        node("[0].id").isValidId
        node("[0].name").isEqualTo("test")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tags key with no tag`() {
    performProjectAuthPut("keys/${testData.noTagKey.id}/tags", TagKeyDto(name = "brand new tag"))
      .andPrettyPrint
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("brand new tag")
      }

    executeInNewTransaction {
      val noTagKey = testData.noTagKey.refresh()
      val tag = noTagKey.keyMeta!!.tags.find { it.name == "brand new tag" }
      assertThat(tag).isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tags key with existing tag`() {
    performProjectAuthPut("keys/${testData.noTagKey.id}/tags", TagKeyDto(name = testData.existingTag.name))
      .andPrettyPrint
      .andAssertThatJson {
        node("id").isEqualTo(testData.existingTag.id)
        node("name").isEqualTo(testData.existingTag.name)
      }
    executeInNewTransaction {
      val noTagKey = testData.noTagKey.refresh()
      val tag = noTagKey.keyMeta!!.tags.find { it.name == testData.existingTag.name }
      assertThat(tag).isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removes tag`() {
    performProjectAuthDelete("keys/${testData.existingTagKey.id}/tags/${testData.existingTag.id}", null)
      .andIsOk

    executeInNewTransaction {
      val existingTagKey = testData.existingTagKey.refresh()
      val tag = existingTagKey.keyMeta!!.tags.find { it.name == testData.existingTag.name }
      assertThat(tag).isNull()
      assertThat(existingTagKey.keyMeta!!.tags).hasSize(2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removes tag from key, keeps tag for another key`() {
    performProjectAuthDelete("keys/${testData.existingTagKey.id}/tags/${testData.existingTag2.id}", null)
      .andIsOk

    executeInNewTransaction {
      // check whether tag is removed from existingTagKey
      val existingTagKey = testData.existingTagKey.refresh()
      val tag = existingTagKey.keyMeta!!.tags.find { it.name == testData.existingTag2.name }
      assertThat(tag).isNull()
      assertThat(existingTagKey.keyMeta!!.tags).hasSize(2)

      // check whether tag is not removed from existingTagKey2
      val existingTagKey2 = testData.existingTagKey2.refresh()
      val tag2 = existingTagKey2.keyMeta!!.tags.find { it.name == testData.existingTag2.name }
      assertThat(tag2).isNotNull
      assertThat(existingTagKey2.keyMeta!!.tags).hasSize(1)
    }
  }

  fun Key.refresh(): Key {
    return keyService.get(this.id)
  }
}
