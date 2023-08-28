package io.tolgee.api.v2.controllers

import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.data.ProjectsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AnnouncementControllerTest() : AuthorizedControllerTest() {
  lateinit var testData: ProjectsTestData

  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @BeforeEach
  fun createData() {
    testData = ProjectsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `announcement is in initial data`() {
    this.currentDateProvider.forceDateString("2023-08-28 00:00:00 UTC")
    this.performAuthGet("/v2/public/initial-data").andIsOk.andPrettyPrint.andAssertThatJson {
      node("announcement.type").isString
    }
  }

  @Test
  fun `announcement will disappear after until time`() {
    this.currentDateProvider.forceDateString("2100-01-01 00:00:00 UTC")
    this.performAuthGet("/v2/public/initial-data").andIsOk.andPrettyPrint.andAssertThatJson {
      node("announcement").isNull()
    }
  }

  @Test
  fun `announcement can be dismissed`() {
    this.currentDateProvider.forceDateString("2023-08-28 00:00:00 UTC")
    this.performAuthPost("/v2/announcement/dismiss", content = null).andIsOk
    this.performAuthGet("/v2/public/initial-data").andIsOk.andPrettyPrint.andAssertThatJson {
      node("announcement").isNull()
    }
  }

}
