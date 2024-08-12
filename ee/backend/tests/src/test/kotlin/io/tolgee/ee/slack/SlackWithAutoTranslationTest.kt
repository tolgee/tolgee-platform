package io.tolgee.ee.slack

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.block.SectionBlock
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
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

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextRecreatingTest
class SlackWithAutoTranslationTest : MachineTranslationTest() {
  @Autowired
  @MockBean
  lateinit var slackClient: Slack

  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
    private const val TRANSLATED_WITH_GOOGLE_RESPONSE = "Translated with Google"
    private const val BASE_LANGUAGE_TAG = "en"
    private const val BASE_LANGUAGE_TRANSLATION = "base text"
  }

  lateinit var testData: SlackTestData

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    initMachineTranslationMocks(1_000)
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
    this.projectSupplier = { testData.projectBuilder.self }
    tolgeeProperties.slack.token = "token"
  }

  fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends auto translated text on a new key added with base translation`() {
    saveTestData()
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    performCreateKey("new key", mapOf(BASE_LANGUAGE_TAG to BASE_LANGUAGE_TRANSLATION)).andIsCreated
    waitForNotThrowing(timeout = 3_000) {
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
      val request = mockedSlackClient.chatPostMessageRequests.first()
      assertThatActualTranslationsEqualToExpected(request)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends auto translated text when base provided (non-existing)`() {
    saveTestData()
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    performSetBaseTranslation(testData.baseTranslationNotExistKey.name)
    waitForNotThrowing(timeout = 3_000) {
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
      val request = mockedSlackClient.chatPostMessageRequests.first()
      assertThatActualTranslationsEqualToExpected(request)
    }
  }

  private fun assertThatActualTranslationsEqualToExpected(request: ChatPostMessageRequest) {
    val actualMap =
      request.attachments
        .dropLast(1).associate {
          val keyLanguage = ((it.blocks[0] as SectionBlock).text.text).removePrefix("null ").trim()
          val keyTranslation = (it.blocks[1] as SectionBlock).text.text
          keyLanguage to keyTranslation
        }

    assertThat(actualMap).isEqualTo(getExpectedMapOfTranslations())
  }

  private fun assertThatKeyAutoTranslated(keyName: String) {
    waitForNotThrowing {
      transactionTemplate.execute {
        val translatedText =
          keyService.get(testData.projectBuilder.self.id, keyName, null)
            .getLangTranslation(testData.secondLanguage).text

        assertThat(
          translatedText,
        ).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
      }
    }
  }

  private fun performSetBaseTranslation(key: String) {
    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(
        key = key,
        translations = mapOf(BASE_LANGUAGE_TAG to BASE_LANGUAGE_TRANSLATION),
      ),
    )
  }

  fun getExpectedMapOfTranslations(baseTranslation: String = BASE_LANGUAGE_TRANSLATION) =
    mapOf(
      "*English* (base)" to baseTranslation,
      "*Czech*" to TRANSLATED_WITH_GOOGLE_RESPONSE,
      "*French*" to TRANSLATED_WITH_GOOGLE_RESPONSE,
    )
}
