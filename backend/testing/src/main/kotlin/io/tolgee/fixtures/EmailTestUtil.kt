package io.tolgee.fixtures

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assertions.Assertions
import org.assertj.core.api.AbstractStringAssert
import org.mockito.Mockito
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

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
    whenever(javaMailSender.createMimeMessage()).thenReturn(JavaMailSenderImpl().createMimeMessage())
    whenever(javaMailSender.send(messageArgumentCaptor.capture())).thenAnswer { }
  }

  val messageContent: String
    get() {
      return (
        (messageArgumentCaptor.firstValue.content as MimeMultipart)
          .getBodyPart(0).content as MimeMultipart
        )
        .getBodyPart(0).content as String
    }

  val assertEmailTo: AbstractStringAssert<*>
    get() {
      @Suppress("CAST_NEVER_SUCCEEDS")
      return Assertions.assertThat(messageArgumentCaptor.firstValue.getHeader("To")[0] as String)
    }
}
