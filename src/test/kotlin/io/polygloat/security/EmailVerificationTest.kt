package io.polygloat.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import io.polygloat.assertions.Assertions.assertThat
import io.polygloat.controllers.AbstractControllerTest
import io.polygloat.dtos.request.SignUpDto
import io.polygloat.exceptions.NotFoundException
import io.polygloat.repository.EmailVerificationRepository
import io.polygloat.service.EmailVerificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest(properties = [
    "polygloat.authentication.needs-email-verification=true",
    "polygloat.front-end-url=dummy_frontend_url"
])
@AutoConfigureMockMvc
class EmailVerificationTest : AbstractControllerTest() {

    @set:Autowired
    lateinit var mailSender: MailSender

    @set:Autowired
    lateinit var emailVerificationService: EmailVerificationService

    @set:Autowired
    lateinit var emailVerificationRepository: EmailVerificationRepository

    @BeforeMethod
    fun setup() {
        every { mailSender.send(any()) } returns Unit
    }

    @Test
    fun doesNotLoginWhenNotVerified() {
        val createUser = dbPopulator.createUser(initialUsername)
        emailVerificationService.createForUser(createUser)

        val response = doAuthentication(initialUsername, initialPassword)
        assertThat(response).error().hasCode("email_not_verified")
    }

    @Test
    fun doesLoginWhenVerified() {
        dbPopulator.createUser(initialUsername)
        val result = doAuthentication(initialUsername, initialPassword)
        assertThat(result.response.status).isEqualTo(200)
    }

    @Test
    fun verifiesEmail() {
        val createUser = dbPopulator.createUser(initialUsername)
        val emailVerification = emailVerificationService.createForUser(createUser)
        mvc.perform(get("/api/public/verify_email/${createUser.id}/${emailVerification!!.code}"))
                .andExpect(status().isOk).andReturn()

        assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isEmpty
    }

    @Test
    fun signUpSavesVerification() {
        val signUpDto = SignUpDto("Test Name", "aaa@aaa.com", "testtest", null)

        mockkConstructor(SimpleMailMessage::class)

        mvc.perform(post("/api/public/sign_up")
                .content(mapper.writeValueAsString(signUpDto))
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()

        val user = userAccountService.getByUserName(signUpDto.email).orElseThrow { NotFoundException() }

        verify { mailSender.send(any()) }

        verify { anyConstructed<SimpleMailMessage>().subject = "Polygloat e-mail verification" }

        verify {
            anyConstructed<SimpleMailMessage>().text = match {
                it.contains("dummy_frontend_url/login/verify_email/${user.id}/")
            }
        }

        assertThat(userAccountService.get(user.id!!)).isNotNull
    }
}

@Configuration
open class MockBeanMailSender {
    @Bean
    @Primary
    open fun mailSender(): MailSender {
        return mockk()
    }
}