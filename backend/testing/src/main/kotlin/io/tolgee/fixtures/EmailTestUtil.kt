package io.tolgee.fixtures

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.assertj.core.api.AbstractStringAssert
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component

@Component
class EmailTestUtil() {
  @Autowired
  @MockBean
  lateinit var javaMailSender: JavaMailSender

  @Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  lateinit var messageArgumentCaptor: KArgumentCaptor<MimeMessage>

  fun initMocks() {
    messageArgumentCaptor = argumentCaptor()
    Mockito.clearInvocations(javaMailSender)
    tolgeeProperties.smtp.from = "aaa@a.a"
    whenever(javaMailSender.createMimeMessage()).thenAnswer {
      JavaMailSenderImpl().createMimeMessage()
    }
    whenever(javaMailSender.send(messageArgumentCaptor.capture())).thenAnswer { }
  }

  val firstMessageContent: String
    get() = messageContents.first()

  val singleEmailContent: String
    get() {
      messageContents.assert.hasSize(1)
      return messageContents.single()
    }

  val messageContents: List<String>
    get() =
      messageArgumentCaptor.allValues.map {
        (
          (it.content as MimeMultipart)
            .getBodyPart(0).content as MimeMultipart
        )
          .getBodyPart(0).content as String
      }

  fun emailToString(email: MimeMessage): String {
    return (
      (email.content as MimeMultipart)
        .getBodyPart(0).content as MimeMultipart
    )
      .getBodyPart(0).content as String
  }

  fun verifyEmailSent() {
    verify(javaMailSender).send(any<MimeMessage>())
  }

  fun verifyTimesEmailSent(num: Int) {
    verify(javaMailSender, times(num)).send(any<MimeMessage>())
  }

  val assertEmailTo: AbstractStringAssert<*>
    get() {
      @Suppress("CAST_NEVER_SUCCEEDS")
      return Assertions.assertThat(messageArgumentCaptor.firstValue.getHeader("To")[0] as String)
    }

  fun findEmail(to: String): MimeMessage? {
    return messageArgumentCaptor.allValues.find { it.getHeader("To")[0] == to }
  }
}
