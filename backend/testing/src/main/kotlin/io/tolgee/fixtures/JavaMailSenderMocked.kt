package io.tolgee.fixtures

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.AbstractStringAssert
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

interface JavaMailSenderMocked {
  var javaMailSender: JavaMailSender
  var messageArgumentCaptor: ArgumentCaptor<MimeMessage>
  var tolgeeProperties: TolgeeProperties

  @BeforeEach
  fun initMocks() {
    Mockito.clearInvocations(javaMailSender)
    tolgeeProperties.smtp.from = "aaa@a.a"
    whenever(javaMailSender.createMimeMessage()).thenReturn(JavaMailSenderImpl().createMimeMessage())
    messageArgumentCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)
  }

  val MimeMessage.tolgeeStandardMessageContent: String
    get() {
      return (
        (this.content as MimeMultipart)
          .getBodyPart(0).content as MimeMultipart
        )
        .getBodyPart(0).content as String
    }

  fun assertEmailTo(): AbstractStringAssert<*> {
    @Suppress("CAST_NEVER_SUCCEEDS")
    return assertThat(messageArgumentCaptor.value.getHeader("To")[0] as String)
  }
}
