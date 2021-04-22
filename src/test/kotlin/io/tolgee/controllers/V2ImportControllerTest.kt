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
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testng.annotations.Test
import javax.servlet.DispatcherType
import javax.servlet.ServletContext

class V2ImportControllerTest : SignedInControllerTest() {
    @Value("classpath:import/zipOfJsons.zip")
    lateinit var zipOfJsons: Resource

    @Value("classpath:import/zipOfUnknown.zip")
    lateinit var zipOfUnknown: Resource

    @Test
    fun `it parses zip file and saves issues`() {
        val repository = dbPopulator.createBase(generateUniqueString())
        commitTransaction()

        performImport(repositoryId = repository.id, mapOf(Pair("zipOfUnknown.zip", zipOfUnknown)))
                .assertContainsMessage(FOUND_FILES_IN_ARCHIVE, listOf(3))
                .assertContainsMessage(FOUND_ARCHIVE)

        importService.find(repository.id, repository.userOwner?.id!!)!!.let { import ->
            assertThat(import.files.size).isEqualTo(3)
            assertThat(import.archives.size).isEqualTo(1)
            assertThat(import.archives[0].name).isEqualTo("zipOfUnknown.zip")
            import.files.forEach {
                assertThat(it.issues).hasSize(1)
                assertThat(it.issues[0].type).isEqualTo(FileIssueType.NO_MATCHING_PROCESSOR)
            }
            assertThat(import.files.map { it.name }).containsAll(listOf("aaaa.unkwn", "aaaa2.unkwn", "aaaa4.unkwn"))
        }
    }

    @Test
    fun `it finds proper `() {
        val repository = dbPopulator.createBase(generateUniqueString())
        commitTransaction()

        performImport(repositoryId = repository.id, mapOf(Pair("zipOfJsons.zip", zipOfJsons)))
                .assertContainsMessage(FOUND_FILES_IN_ARCHIVE, listOf(3))
                .assertContainsMessage(FOUND_ARCHIVE)

        importService.find(repository.id, repository.userOwner?.id!!)!!.let { importEntity ->
            assertThat(importEntity.files.size).isEqualTo(3)
            assertThat(importEntity.archives.size).isEqualTo(1)
            assertThat(importEntity.archives[0].name).isEqualTo("zipOfJsons.zip")
            assertThat(importEntity.files.map { it.name }).containsAll(listOf("en.json", "cs.json", "fr.json"))
            val keys = importService.findKeys(importEntity)
            keys.forEach { key ->
                assertThat(keys.filter { it.name == key.name })
                        .describedAs("Each key is stored just once")
                        .hasSizeLessThan(2)

                key.translations.forEach {
                    assertThat(it.issues).hasSize(0)
                }
            }
            importEntity.files.forEach {
                assertThat(it.issues).hasSize(0)
            }
            assertThat(keys).hasSize(206)
        }
    }

    private fun performImport(repositoryId: Long, files: Map<String?, Resource>): ResultActions {
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
            return mvc.perform(asyncDispatch(it))
        }
    }

    private fun ResultActions.assertContainsMessage(
            type: ImportStreamingProgressMessageType,
            params: List<Any?>? = null)
            : ResultActions {
        this.andReturn().response.contentAsString.removeSuffix(";;;").split(";;;").any {
            val mapped = jacksonObjectMapper().readValue<ImportStreamingProgressMessage>(it)
            ImportStreamingProgressMessage(type, params) == mapped
        }.let {
            assertThat(it).describedAs("""Streaming response contains message of type ${type} 
                |with params ${params.toString()}
            """.trimMargin()).isTrue
        }
        return this
    }

    private fun asyncDispatch(mvcResult: MvcResult): RequestBuilder {
        mvcResult.getAsyncResult(10000)
        return RequestBuilder { servletContext: ServletContext? ->
            val request = mvcResult.request
            request.dispatcherType = DispatcherType.ASYNC
            request.isAsyncStarted = false
            request
        }
    }
}
