package io.tolgee.controllers

import io.tolgee.annotations.RepositoryJWTAuthTestMethod
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Organization
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest : RepositoryAuthControllerTest() {

    @Test
    @RepositoryJWTAuthTestMethod
    fun testCreate() {
        (0..50).forEach {
            organizationRepository.save(Organization(name = "Org - $it", addressPart = "org$it"))
        }

        val result = performRepositoryAuthGet("/organisations").andIsOk.andReturn()
        assertThat(result)
    }
}
