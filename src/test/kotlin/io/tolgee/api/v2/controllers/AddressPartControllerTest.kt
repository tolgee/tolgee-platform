package io.tolgee.api.v2.controllers

import io.tolgee.controllers.SignedInControllerTest
import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.GenerateAddressPathDto
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Organization
import io.tolgee.model.Repository
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

import org.testng.Assert.*

@SpringBootTest
@AutoConfigureWebMvc
class AddressPartControllerTest : SignedInControllerTest() {

    @Test
    fun testValidateOrganizationAddressPart() {
        performAuthGet("/v2/address-part/validate-organization/hello-1").andIsOk.andAssertThatJson.isEqualTo(true)
        organizationRepository.save(
                Organization(
                        name = "aaa",
                        addressPart = "hello-1"
                )
        )
        performAuthGet("/v2/address-part/validate-organization/hello-1").andIsOk.andAssertThatJson.isEqualTo(false)
    }

    @Test
    fun testValidateRepositoryAddressPart() {
        performAuthGet("/v2/address-part/validate-repository/hello-1").andIsOk.andAssertThatJson.isEqualTo(true)
        repositoryRepository.save(
                Repository(
                        name = "aaa",
                        addressPart = "hello-1"
                ).also { it.userOwner = dbPopulator.createUserIfNotExists("hello") }
        )
        performAuthGet("/v2/address-part/validate-repository/hello-1").andIsOk.andAssertThatJson.isEqualTo(false)
    }


    @Test
    fun testGenerateOrganizationAddressPart() {
        performAuthPost("/v2/address-part/generate-organization", GenerateAddressPathDto("Hello world"))
                .andIsOk.andAssertThatJson.isEqualTo("hello-world")

        organizationRepository.save(
                Organization(
                        name = "aaa",
                        addressPart = "hello-world"
                )
        )

        performAuthPost("/v2/address-part/generate-organization", GenerateAddressPathDto("Hello world"))
                .andIsOk.andAssertThatJson.isEqualTo("hello-world1")
    }

    @Test
    fun testGenerateRepositoryAddressPart() {
        repositoryRepository.save(
                Repository(
                        name = "aaa",
                        addressPart = "hello-world"
                ).also { it.userOwner = dbPopulator.createUserIfNotExists("hello") }
        )
        performAuthPost("/v2/address-part/generate-repository", GenerateAddressPathDto("Hello world"))
                .andIsOk.andAssertThatJson.isEqualTo("hello-world1")
    }
}
