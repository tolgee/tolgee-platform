package io.tolgee

import com.google.cloud.translate.Translate
import com.google.cloud.translate.Translation
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.ResultActions
import software.amazon.awssdk.services.translate.TranslateClient
import software.amazon.awssdk.services.translate.model.TranslateTextRequest
import software.amazon.awssdk.services.translate.model.TranslateTextResponse

class MachineTranslationTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  @MockBean
  lateinit var googleTranslate: Translate

  @Autowired
  @MockBean
  lateinit var amazonTranslate: TranslateClient

  fun initMachineTranslationMocks(translateDelay: Long = 0) {
    Mockito.reset(googleTranslate)
    Mockito.reset(amazonTranslate)
    val googleTranslationMock = mock() as Translation
    val awsTranslateTextResult = TranslateTextResponse
      .builder().translatedText("Translated with Amazon").build()

    whenever(
      googleTranslate.translate(
        any<String>(),
        any(),
        any(),
        any()
      )
    ).thenReturn(googleTranslationMock)

    whenever(googleTranslationMock.translatedText).then {
      Thread.sleep(translateDelay)
      return@then "Translated with Google"
    }

    whenever(amazonTranslate.translateText(any<TranslateTextRequest>())).thenReturn(awsTranslateTextResult)
  }

  fun Key.getLangTranslation(lang: Language): io.tolgee.model.translation.Translation {
    return transactionTemplate.execute {
      keyService.get(this.id).translations.find {
        it.language == lang
      } ?: throw IllegalStateException("Translation not found")
    }!!
  }

  protected fun createAnotherThisIsBeautifulKey() {
    performCreateKey(
      translations = mapOf(
        "en" to "This is beautiful",
      )
    ).andIsCreated
  }

  protected fun performCreateKey(keyName: String = CREATE_KEY_NAME, translations: Map<String, String>): ResultActions {
    return performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = CREATE_KEY_NAME,
        translations = translations
      )
    )
  }

  companion object {
    const val CREATE_KEY_NAME = "super_key"
  }
}
