package io.polygloat.configuration.polygloat

import io.polygloat.assertions.Assertions.assertThat
import io.polygloat.service.UserAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test

@AutoConfigureMockMvc
@SpringBootTest(properties = [
    "polygloat.authentication.jwtSecret=test_jwt_secret"
])
class AuthenticationPropertiesTest : AbstractTestNGSpringContextTests() {
    @set:Autowired
    lateinit var polygloatProperties: PolygloatProperties

    @set:Autowired
    lateinit var userAccountService: UserAccountService

    @Test
    fun testCreateInitialUserDisabled() {
        assertThat(polygloatProperties.authentication.createInitialUser).isEqualTo(false)
        assertThat(userAccountService.getByUserName(polygloatProperties.authentication.initialUsername)).isEmpty
    }


    @Test
    fun testJwtProperty() {
        assertThat(polygloatProperties.authentication.jwtSecret).isEqualTo("test_jwt_secret")
    }
}