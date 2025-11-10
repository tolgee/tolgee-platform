package io.tolgee.testing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.model.Invitation
import io.tolgee.service.invitation.InvitationService
import org.springframework.context.ApplicationContext
import org.springframework.test.web.servlet.ResultActions

class InvitationTestUtil(
  private val test: ProjectAuthControllerTest,
  private val applicationContext: ApplicationContext,
) {
  fun perform(fn: ProjectInviteUserDto.(getLang: LangByTag) -> Unit): ResultActions {
    val testData = prepareTestData()

    return test.performProjectAuthPut(
      "/invite",
      ProjectInviteUserDto().apply {
        fn(
          this,
        ) { tag ->
          testData.projectBuilder.data.languages
            .find { it.self.tag == tag }
            ?.self
            ?.id
            ?: throw NullPointerException("Language $tag not found")
        }
      },
    )
  }

  private fun prepareTestData(): BaseTestData {
    val testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    test.projectSupplier = { testData.projectBuilder.self }
    test.userAccount = testData.user
    return testData
  }

  fun getInvitation(actions: ResultActions): Invitation {
    val key = parseCode(actions.andGetContentAsString)
    return invitationService.getInvitation(key)
  }

  private val testDataService: TestDataService
    get() = applicationContext.getBean(TestDataService::class.java)

  private val invitationService: InvitationService
    get() = applicationContext.getBean(InvitationService::class.java)

  private fun parseCode(invitationJson: String) =
    jacksonObjectMapper().readValue<Map<String, Any>>(invitationJson)["code"] as String
}
