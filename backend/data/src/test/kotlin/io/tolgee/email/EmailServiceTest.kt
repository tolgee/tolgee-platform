/**
 * Copyright (C) 2024 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.email

import io.tolgee.configuration.tolgee.SmtpProperties
import io.tolgee.testing.assert
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@Component
@ExtendWith(SpringExtension::class)
@Import(EmailService::class, EmailTemplateConfig::class)
class EmailServiceTest {
  @MockBean
  private lateinit var smtpProperties: SmtpProperties

  @MockBean
  private lateinit var mailSender: JavaMailSender

  @Autowired
  private lateinit var emailService: EmailService

  @Captor
  private lateinit var emailCaptor: ArgumentCaptor<MimeMessage>

  @BeforeEach
  fun beforeEach() {
    val sender = JavaMailSenderImpl()
    whenever(smtpProperties.from).thenReturn("Tolgee Test <robomouse+test@tolgee.internal>")
    whenever(mailSender.createMimeMessage()).let {
      val msg = sender.createMimeMessage()
      it.thenReturn(msg)
    }
  }

  @Test
  fun `it sends a rendered email with variables and ICU strings processed`() {
    emailService.sendEmailTemplate("test@tolgee.internal", "zz-test-email", Locale.ENGLISH, TEST_PROPERTIES)
    verify(mailSender).send(emailCaptor.capture())

    val email = emailCaptor.value
    email.subject.assert.isEqualTo("Test email (written with React Email)")
    email.allRecipients.asList().assert.singleElement().asString().isEqualTo("test@tolgee.internal")

    email.content
      .let { it as MimeMultipart }
      .let { it.getBodyPart(0).content as MimeMultipart }
      .let { it.getBodyPart(0).content as String }
      .assert
      .contains("Value of `testVar` : <span>test!!</span>")
      .contains("<span>Was `testVar` equal to &quot;meow&quot; : </span><span>no</span>")
      .contains("Powered by")
      .doesNotContain(" th:")
      .doesNotContain(" data-th")
  }

  @Test
  fun `it correctly processes conditional blocks`() {
    // FWIW this is very close to just testing Thymeleaf itself, but it serves as a sanity check for the template itself
    emailService.sendEmailTemplate("test@tolgee.internal", "zz-test-email", Locale.ENGLISH, TEST_PROPERTIES_MEOW)
    verify(mailSender).send(emailCaptor.capture())

    val email = emailCaptor.value
    email.content
      .let { it as MimeMultipart }
      .let { it.getBodyPart(0).content as MimeMultipart }
      .let { it.getBodyPart(0).content as String }
      .assert
      .contains("Value of `testVar` : <span>meow</span>")
      .contains("<span>Was `testVar` equal to &quot;meow&quot; : </span><span>yes</span>")
  }

  companion object {
    private val TEST_PROPERTIES = mapOf(
      "testVar" to "test!!",
      "testList" to listOf(
        mapOf("name" to "Name #1"),
        mapOf("name" to "Name #2"),
        mapOf("name" to "Name #3"),
      )
    )

    private val TEST_PROPERTIES_MEOW = mapOf(
      "testVar" to "meow",
      "testList" to listOf(
        mapOf("name" to "Name #1"),
        mapOf("name" to "Name #2"),
        mapOf("name" to "Name #3"),
      )
    )
  }
}
