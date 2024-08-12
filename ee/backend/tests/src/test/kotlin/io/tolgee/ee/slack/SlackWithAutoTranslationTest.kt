package io.tolgee.ee.slack

import com.slack.api.Slack
import com.slack.api.model.block.SectionBlock
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import kotlin.random.Random

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextRecreatingTest
class SlackWithAutoTranslationTest : MachineTranslationTest() {
  @Autowired
  @MockBean
  lateinit var slackClient: Slack

  @Autowired
  lateinit var slackMessageService: SavedSlackMessageService

  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
    private const val TRANSLATED_WITH_GOOGLE_RESPONSE = "Translated with Google"
  }

  lateinit var testData: SlackTestData

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    this.projectSupplier = { testData.projectBuilder.self }
    tolgeeProperties.slack.token = "token"
  }

  fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `auto translation`() {
    saveTestData()
    initMachineTranslationMocks(Random.nextLong(500, 1_000))
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    performCreateKey("new key", mapOf("en" to "base text")).andIsCreated
    waitForNotThrowing(timeout = 3_000) {
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
      val request = mockedSlackClient.chatPostMessageRequests.first()
      val actualMap =
        request.attachments
          .dropLast(1).associate {
            val keyText = ((it.blocks[0] as SectionBlock).text.text).removePrefix("null ").trim()
            val valueText = (it.blocks[1] as SectionBlock).text.text
            keyText to valueText
          }

      assertThat(actualMap).isEqualTo(getExpectedMapOfTranslations("base text"))
    }
  }

  private fun assertThatKeyAutoTranslated(keyName: String) {
    waitForNotThrowing {
      transactionTemplate.execute {
        val translatedText =
          keyService.get(testData.projectBuilder.self.id, keyName, null)
            .getLangTranslation(testData.secondLanguage).text

        translatedText
        assertThat(
          translatedText,
        ).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
      }
    }
  }

  fun getExpectedMapOfTranslations(baseTranslation: String) =
    mapOf(
      "*English* (base)" to baseTranslation,
      "*Czech*" to TRANSLATED_WITH_GOOGLE_RESPONSE,
      "*French*" to TRANSLATED_WITH_GOOGLE_RESPONSE,
    )
}
