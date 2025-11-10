package io.tolgee.api.v2.controllers.administration

import io.tolgee.development.testDataBuilder.data.AdministrationTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectBatchLockControllerTest : AuthorizedControllerTest() {
  lateinit var testData: AdministrationTestData

  @BeforeEach
  fun createData() {
    testData = AdministrationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
  }

  @Test
  fun `GET project-batch-locks returns forbidden without super auth`() {
    // Test without admin user
    userAccount = testData.user
    performAuthGet("/v2/administration/project-batch-locks")
      .andIsForbidden
  }

  @Test
  fun `GET project-batch-locks returns locks with super auth`() {
    // Test with admin user - should succeed and return valid CollectionModel response
    val response =
      performAuthGet("/v2/administration/project-batch-locks")
        .andIsOk
        .andReturn()
        .response.contentAsString

    // Verify the response structure
    performAuthGet("/v2/administration/project-batch-locks")
      .andIsOk
      .andAssertThatJson {
        // Response should be a valid JSON object (either empty {} or populated CollectionModel)
        isObject
      }

    // Additional verification: ensure the endpoint is properly configured
    // Empty response {} is valid when there are no project batch locks
    // Non-empty response should follow CollectionModel structure with _embedded
    assert(response == "{}" || response.contains("_embedded")) {
      "Response should be either empty CollectionModel {} or contain _embedded structure, got: $response"
    }
  }

  @Test
  fun `GET batch-job-queue returns queue items with super auth`() {
    // Test with admin user - should succeed and return valid CollectionModel response
    val response =
      performAuthGet("/v2/administration/batch-job-queue")
        .andIsOk
        .andReturn()
        .response.contentAsString

    // Verify the response structure
    performAuthGet("/v2/administration/batch-job-queue")
      .andIsOk
      .andAssertThatJson {
        // Response should be a valid JSON object (either empty {} or populated CollectionModel)
        isObject
      }

    // Additional verification: ensure the endpoint is properly configured
    // Empty response {} is valid when there are no batch job queue items
    // Non-empty response should follow CollectionModel structure with _embedded
    assert(response == "{}" || response.contains("_embedded")) {
      "Response should be either empty CollectionModel {} or contain _embedded structure, got: $response"
    }
  }

  @Test
  fun `GET batch-job-queue returns forbidden without super auth`() {
    // Test without admin user
    userAccount = testData.user
    performAuthGet("/v2/administration/batch-job-queue")
      .andIsForbidden
  }
}
