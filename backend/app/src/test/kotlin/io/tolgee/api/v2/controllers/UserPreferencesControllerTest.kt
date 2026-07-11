package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserPreferencesControllerTest : AuthorizedControllerTest() {
  lateinit var testData: OrganizationTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `stores language`() {
    userAccount = testData.franta
    performAuthPut("/v2/user-preferences/set-language/de", null).andIsOk
    transactionTemplate.execute {
      assertThat(userAccountService.findActive(userAccount!!.username)?.preferences?.language).isEqualTo("de")
    }
  }

  @Test
  fun `returns preferences`() {
    userAccount = testData.pepa
    performAuthGet("/v2/user-preferences").andIsOk.andAssertThatJson {
      node("language").isEqualTo("de")
      node("preferredOrganizationId").isEqualTo(testData.pepaOrg.id)
    }
  }

  @Test
  fun `returns already stored preferences`() {
    userAccount = testData.jirina
    performAuthGet("/v2/user-preferences").andIsOk.andAssertThatJson {
      node("language").isEqualTo("ft")
      node("preferredOrganizationId").isEqualTo(testData.jirinaOrg.id)
    }
  }

  @Test
  fun `stores storage json field`() {
    userAccount = testData.franta
    performAuthPut("/v2/user-preferences/storage/testField", "testValue").andIsOk
    transactionTemplate.execute {
      val preferences = userAccountService.findActive(userAccount!!.username)?.preferences
      assertThat(preferences?.storageJson).isNotNull
      assertThat(preferences?.storageJson?.get("testField")).isEqualTo("testValue")
    }
  }

  @Test
  fun `preserves existing storage json data when setting new field`() {
    userAccount = testData.franta
    // Set first field
    performAuthPut("/v2/user-preferences/storage/field1", "value1").andIsOk
    // Set second field
    performAuthPut("/v2/user-preferences/storage/field2", "value2").andIsOk

    transactionTemplate.execute {
      val preferences = userAccountService.findActive(userAccount!!.username)?.preferences
      assertThat(preferences?.storageJson).isNotNull
      assertThat(preferences?.storageJson?.get("field1")).isEqualTo("value1")
      assertThat(preferences?.storageJson?.get("field2")).isEqualTo("value2")
    }
  }

  @Test
  fun `overwrites existing storage json field`() {
    userAccount = testData.franta
    // Set initial value
    performAuthPut("/v2/user-preferences/storage/testField", "initialValue").andIsOk
    // Update the same field
    performAuthPut("/v2/user-preferences/storage/testField", "updatedValue").andIsOk

    transactionTemplate.execute {
      val preferences = userAccountService.findActive(userAccount!!.username)?.preferences
      assertThat(preferences?.storageJson).isNotNull
      assertThat(preferences?.storageJson?.get("testField")).isEqualTo("updatedValue")
    }
  }

  @Test
  fun `handles empty string value`() {
    userAccount = testData.franta
    performAuthPut("/v2/user-preferences/storage/emptyField", "").andIsOk

    transactionTemplate.execute {
      val preferences = userAccountService.findActive(userAccount!!.username)?.preferences
      assertThat(preferences?.storageJson).isNotNull
      assertThat(preferences?.storageJson?.get("emptyField")).isEqualTo("")
    }
  }

  @Test
  fun `returns null when field does not exist`() {
    userAccount = testData.franta
    performAuthGet("/v2/user-preferences/storage/nonExistentField").andIsOk.andAssertThatJson {
      node("data").isEqualTo(null)
    }
  }

  @Test
  fun `returns stored storage field`() {
    userAccount = testData.franta
    performAuthPut("/v2/user-preferences/storage/testField", "testValue").andIsOk

    performAuthGet("/v2/user-preferences/storage/testField").andIsOk.andAssertThatJson {
      node("data").isEqualTo("testValue")
    }
  }

  @Test
  fun `returns specific fields with different data types`() {
    userAccount = testData.franta
    performAuthPut("/v2/user-preferences/storage/stringField", "value").andIsOk
    performAuthPut("/v2/user-preferences/storage/numberField", 42).andIsOk
    performAuthPut("/v2/user-preferences/storage/booleanField", true).andIsOk

    performAuthGet("/v2/user-preferences/storage/stringField").andIsOk.andAssertThatJson {
      node("data").isEqualTo("value")
    }
    performAuthGet("/v2/user-preferences/storage/numberField").andIsOk.andAssertThatJson {
      node("data").isEqualTo(42)
    }
    performAuthGet("/v2/user-preferences/storage/booleanField").andIsOk.andAssertThatJson {
      node("data").isEqualTo(true)
    }
  }

  @Test
  fun `returns existing field after setting multiple fields`() {
    userAccount = testData.franta
    performAuthPut("/v2/user-preferences/storage/field1", "value1").andIsOk
    performAuthPut("/v2/user-preferences/storage/field2", "value2").andIsOk

    // Verify we can retrieve individual fields
    performAuthGet("/v2/user-preferences/storage/field1").andIsOk.andAssertThatJson {
      node("data").isEqualTo("value1")
    }
    performAuthGet("/v2/user-preferences/storage/field2").andIsOk.andAssertThatJson {
      node("data").isEqualTo("value2")
    }
  }
}
