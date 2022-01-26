package io.tolgee

import com.amazonaws.services.translate.AmazonTranslate
import com.amazonaws.services.translate.model.TranslateTextResult
import com.google.cloud.translate.Translate
import com.google.cloud.translate.Translation
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

interface MachineTranslationTest {
  var googleTranslate: Translate

  var amazonTranslate: AmazonTranslate

  fun initMachineTranslationMocks(translateDelay: Long = 0) {
    val googleTranslationMock = mock() as Translation
    val awsTranslateTextResult = mock() as TranslateTextResult

    whenever(
      googleTranslate.translate(
        any() as String,
        any() as Translate.TranslateOption,
        any() as Translate.TranslateOption
      )
    ).thenReturn(googleTranslationMock)

    whenever(googleTranslationMock.translatedText).then {
      Thread.sleep(translateDelay)
      return@then "Translated with Google"
    }

    whenever(amazonTranslate.translateText(any())).thenReturn(awsTranslateTextResult)

    whenever(awsTranslateTextResult.translatedText).then {
      Thread.sleep(translateDelay)
      return@then "Translated with Amazon"
    }
  }
}
