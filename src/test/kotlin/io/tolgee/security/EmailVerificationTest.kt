package io.tolgee.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.controllers.AbstractControllerTest
import io.tolgee.dtos.request.SignUpDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.repository.EmailVerificationRepository
import io.tolgee.service.EmailVerificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest(properties = [
    "tolgee.authentication.needs-email-verification=true",
    "tolgee.front-end-url=dummy_frontend_url"
])
@AutoConfigureMockMvc
open class EmailVerificationTest : AbstractControllerTest() {

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

    @BeforeMethod
    fun setupConstructor() {
        mockkConstructor(SimpleMailMessage::class)
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
    fun doesNotVerifyWithWrongCode() {
        val createUser = dbPopulator.createUser(initialUsername)
        val emailVerification = emailVerificationService.createForUser(createUser)
        mvc.perform(get("/api/public/verify_email/${createUser.id}/wrong_code"))
                .andExpect(status().isNotFound).andReturn()

        assertThat(emailVerificationRepository.findById(emailVerification!!.id!!)).isPresent
    }

    @Test
    fun doesNotVerifyWithWrongUser() {
        val createUser = dbPopulator.createUser(initialUsername)
        val emailVerification = emailVerificationService.createForUser(createUser)
        mvc.perform(get("/api/public/verify_email/${createUser.id!! + 1L}/${emailVerification!!.code}"))
                .andExpect(status().isNotFound).andReturn()

        assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isPresent
    }


    val signUpDto = SignUpDto("Test Name", "aaa@aaa.com", "testtest", null)

    protected fun perform(): MvcResult {
        return mvc.perform(post("/api/public/sign_up")
                .content(mapper.writeValueAsString(signUpDto))
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
    }

    @Test
    fun signUpSavesVerification() {
        perform()

        val user = userAccountService.getByUserName(signUpDto.email).orElseThrow { NotFoundException() }

        verify { mailSender.send(any()) }

        verify { anyConstructed<SimpleMailMessage>().subject = "Tolgee e-mail verification" }

        verify {
            anyConstructed<SimpleMailMessage>().text = match {
                it.contains("dummy_frontend_url/login/verify_email/${user.id}/")
            }
        }

        assertThat(userAccountService[user.id!!]).isNotNull
    }

    @Test
    fun signUpFrontendUrlIsPrimary() {
        signUpDto.callbackUrl = "dummyCallbackUrl"

        perform()

        val user = userAccountService.getByUserName(signUpDto.email).orElseThrow { NotFoundException() }

        verify {
            anyConstructed<SimpleMailMessage>().text = match {
                it.contains("dummy_frontend_url/login/verify_email/${user.id}/")
            }
        }

        assertThat(userAccountService[user.id!!]).isNotNull
    }

    @Test
    fun signUpDoesNotReturnToken() {
        assertThat(perform().response.contentAsString).isEqualTo("{}")
    }

    @SpringBootTest(properties = [
        "tolgee.authentication.needs-email-verification=true",
    ])
    @AutoConfigureMockMvc
    class EmailVerificationNoFrontendUrlTest : AbstractControllerTest() {

        val signUpDto = SignUpDto("Test Name", "aaa@aaa.com", "testtest", null)

        @set:Autowired
        lateinit var mailSender: MailSender

        @Test
        fun usesCallbackUrlIs() {
            mockkConstructor(SimpleMailMessage::class)

            every { mailSender.send(any()) } returns Unit

            signUpDto.callbackUrl = "dummyCallbackUrl"


            mvc.perform(post("/api/public/sign_up")
                    .content(mapper.writeValueAsString(signUpDto))
                    .accept(MediaType.ALL)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn()


            val user = userAccountService.getByUserName(signUpDto.email).orElseThrow { NotFoundException() }

            verify {
                anyConstructed<SimpleMailMessage>().text = match {
                    it.contains("dummyCallbackUrl/login/verify_email/${user.id}/")
                }
            }

            assertThat(userAccountService[user.id!!]).isNotNull
        }
    }
}

@Configuration
open class MockBeanMailSender {
    @Bean
    @Primary
    open fun mailSender(): JavaMailSender {
        return mockk()
    }
}
