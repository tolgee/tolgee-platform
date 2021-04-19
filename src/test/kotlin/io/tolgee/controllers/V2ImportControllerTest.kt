package io.tolgee.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessage
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_ARCHIVE
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_FILES_IN_ARCHIVE
import io.tolgee.fixtures.LoggedRequestFactory
import io.tolgee.fixtures.generateUniqueString
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testng.annotations.Test

class V2ImportControllerTest : SignedInControllerTest() {
    @Value("classpath:import/zipOfJsons.zip")
    lateinit var zipFile: Resource

    @Test()
    fun `it parses zip file`() {
        val repository = dbPopulator.createBase(generateUniqueString())
        commitTransaction()

        performImport(repositoryId = repository.id, mapOf(Pair("zipOfJsons.zip", zipFile)))
                .assertContainsMessage(FOUND_FILES_IN_ARCHIVE, listOf(3))
                .assertContainsMessage(FOUND_ARCHIVE)

        importService.find(repository.id, repository.userOwner?.id!!)!!.let { import ->
            assertThat(import.files.size).isEqualTo(3)
            assertThat(import.archives.size).isEqualTo(1)
        }
    }


    fun performImport(repositoryId: Long, files: Map<String?, Resource>): ResultActions {
        val builder = MockMvcRequestBuilders
                .multipart("/v2/repositories/${repositoryId}/import")

        files.forEach {
            builder.file(MockMultipartFile(
                    "files", it.key, "application/zip",
                    it.value.file.readBytes()
            ))
        }

        mvc.perform(LoggedRequestFactory.addToken(
                builder
        )).andReturn().let {
            return mvc.perform(MockMvcRequestBuilders.asyncDispatch(it))
        }
    }

    private fun ResultActions.assertContainsMessage(
            type: ImportStreamingProgressMessageType,
            params: List<Any?>? = null)
            : ResultActions {
        this.andReturn().response.contentAsString.split(";;;").any {
            val mapped = jacksonObjectMapper().readValue<ImportStreamingProgressMessage>(it)
            ImportStreamingProgressMessage(type, params) == mapped
        }.let {
            assertThat(it).describedAs("Streaming response contains message").isTrue
        }
        return this
    }
}
