package io.tolgee.security

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.controllers.SignedInControllerTest
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.security.repository_auth.RepositoryHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

@AutoConfigureMockMvc
class RepositoryPermissionFilterTest : SignedInControllerTest() {

    @field:Autowired
    lateinit var repositoryHolder: RepositoryHolder

    @Test
    fun allowsAccessToPrivilegedUser() {
        val base = dbPopulator.createBase(generateUniqueString())
        performAuthGet("/api/repository/${base.id}/translations/en")
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    }

    @Test
    fun repositoryIdIsRequestScoped() {
        val base = dbPopulator.createBase(generateUniqueString())
        performAuthGet("/api/repository/${base.id}/translations/en")
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        assertThat(repositoryHolder.isRepositoryInitialized).isFalse
    }


    @Test
    fun deniesAccessToNonPrivilegedUser() {
        val base2 = dbPopulator.createBase(generateUniqueString(), "newUser")
        performAuthGet("/api/repository/${base2.id}/translations/en")
                .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
    }

    @Test
    fun returnsNotFoundWhenRepositoryNotExist() {
        val user = dbPopulator.createUserIfNotExists("newUser")
        performAuthGet("/api/repository/${user.id}/translations/en")
                .andExpect(MockMvcResultMatchers.status().isNotFound).andReturn()
    }
}
