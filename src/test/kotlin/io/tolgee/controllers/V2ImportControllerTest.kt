package io.tolgee.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessage
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_ARCHIVE
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_FILES_IN_ARCHIVE
import io.tolgee.fixtures.*
import io.tolgee.model.Repository
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testng.annotations.Test
import javax.servlet.DispatcherType

class V2ImportControllerTest : SignedInControllerTest() {
    @Value("classpath:import/zipOfJsons.zip")
    lateinit var zipOfJsons: Resource

    @Value("classpath:import/zipOfUnknown.zip")
    lateinit var zipOfUnknown: Resource

    @Test
    fun `it parses zip file and saves issues`() {
        val repository = dbPopulator.createBase(generateUniqueString())
        commitTransaction()

        performStreamingImport(repositoryId = repository.id, mapOf(Pair("zipOfUnknown.zip", zipOfUnknown)))
                .andAssertContainsMessage(FOUND_FILES_IN_ARCHIVE, listOf(3))
                .andAssertContainsMessage(FOUND_ARCHIVE).andPrettyPrintStreamingResult().andAssertStreamingResultJson {
                    node("page.totalElements").isEqualTo(0)
                }

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
    fun `it saves proper data and returns correct response `() {
        val repository = dbPopulator.createBase(generateUniqueString())
        commitTransaction()

        performImport(repositoryId = repository.id, mapOf(Pair("zipOfJsons.zip", zipOfJsons)))
                .andPrettyPrint.andAssertThatJson {
                    node("_embedded.languages").isArray.hasSize(3)
                }
        validateSavedImportData(repository)
    }

    @Test
    fun `it saves proper data and returns correct response (streamed)`() {
        val repository = dbPopulator.createBase(generateUniqueString())
        commitTransaction()

        performStreamingImport(repositoryId = repository.id, mapOf(Pair("zipOfJsons.zip", zipOfJsons)))
                .andAssertContainsMessage(FOUND_FILES_IN_ARCHIVE, listOf(3))
                .andAssertContainsMessage(FOUND_ARCHIVE).andPrettyPrintStreamingResult().andAssertStreamingResultJson {
                    node("_embedded.languages").isArray.hasSize(3)
                }
        validateSavedImportData(repository)
    }

    private fun validateSavedImportData(repository: Repository) {
        importService.find(repository.id, repository.userOwner?.id!!)!!.let { importEntity ->
            entityManager.refresh(importEntity)
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

    @Test
    fun `it returns correct result data`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)

        logAsUser(testData.root.data.userAccounts[0].self.username!!, "admin")

        performAuthGet("/v2/repositories/${testData.repository.id}/import/result")
                .andPrettyPrint.andAssertThatJson.node("_embedded.languages").let { languages ->
                    languages.isArray.isNotEmpty
                    languages.node("[0]").let {
                        it.node("name").isEqualTo("en")
                        it.node("existingLanguageName").isEqualTo("English")
                        it.node("importFileName").isEqualTo("multilang.json")
                        it.node("totalCount").isEqualTo("6")
                        it.node("conflictCount").isEqualTo("4")
                    }
                }
    }

    @Test
    fun `it paginates result`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)

        logAsUser(testData.root.data.userAccounts[0].self.username!!, "admin")

        performAuthGet("/v2/repositories/${testData.repository.id}/import/result?page=0&size=2")
    .andPrettyPrint.andAssertThatJson.node("_embedded.languages").isArray.isNotEmpty.hasSize(2)
    }

    @Test
    fun `it return correct translation data`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)

        logAsUser(testData.root.data.userAccounts[0].self.username!!, "admin")

        performAuthGet("/v2/repositories/${testData.repository.id}" +
                "/import/result/languages/${testData.importEnglish.id}/translations").andIsOk
                .andPrettyPrint.andAssertThatJson.node("_embedded.translations").let { translations ->
                    translations.isArray.isNotEmpty.hasSize(4)
                    translations.node("[0]").let {
                        it.node("id").isNotNull
                        it.node("text").isEqualTo("test translation")
                        it.node("keyName").isEqualTo("cool_key")
                        it.node("keyId").isNotNull
                        it.node("collisionId").isNotNull
                        it.node("collisionText").isEqualTo("What a text")
                        it.node("override").isEqualTo(false)
                    }
                }
    }


    @Test
    fun `it pages translation data`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)

        logAsUser(testData.root.data.userAccounts[0].self.username!!, "admin")

        performAuthGet("/v2/repositories/${testData.repository.id}" +
                "/import/result/languages/${testData.importEnglish.id}/translations?size=2").andIsOk
                .andPrettyPrint.andAssertThatJson.node("_embedded.translations").isArray.hasSize(2)
    }


    @Test
    fun `it disables onlyCollision filter translation data`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)

        logAsUser(testData.root.data.userAccounts[0].self.username!!, "admin")

        performAuthGet("/v2/repositories/${testData.repository.id}" +
                "/import/result/languages/${testData.importEnglish.id}/translations?onlyCollisions=false").andIsOk
                .andPrettyPrint.andAssertThatJson.node("_embedded.translations").isArray.hasSize(6)
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

        return mvc.perform(LoggedRequestFactory.addToken(builder))
    }

    private fun performStreamingImport(repositoryId: Long, files: Map<String?, Resource>): ResultActions {
        val builder = MockMvcRequestBuilders
                .multipart("/v2/repositories/${repositoryId}/import/with-streaming-response")

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

    private fun ResultActions.andAssertContainsMessage(
            type: ImportStreamingProgressMessageType,
            params: List<Any?>? = null)
            : ResultActions {
        this.andReturn().response.contentAsString.removeSuffix(";;;").split(";;;").any {
            val mapped = jacksonObjectMapper().readValue<ImportStreamingProgressMessage>(it)
            ImportStreamingProgressMessage(type, params) == mapped
        }.let {
            assertThat(it).describedAs("""Streaming response contains message of type $type 
                |with params ${params.toString()}
            """.trimMargin()).isTrue
        }
        return this
    }

    private fun ResultActions.andAssertStreamingResultJson(jsonAssert: JsonAssert.ConfigurableJsonAssert.() -> Unit)
            : ResultActions {
        val rawResult = this.andReturn().response.contentAsString
                .removeSuffix(";;;").split(";;;").last()
        jsonAssert(assertThatJson(rawResult))
        return this
    }

    private fun ResultActions.andPrettyPrintStreamingResult()
            : ResultActions {
        val rawResult = this.andReturn().response.contentAsString
                .removeSuffix(";;;").split(";;;").last()
        val parsed = jacksonObjectMapper().readValue<Any>(rawResult)
        println(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(parsed))
        return this
    }

    private fun asyncDispatch(mvcResult: MvcResult): RequestBuilder {
        mvcResult.getAsyncResult(10000)
        return RequestBuilder {
            val request = mvcResult.request
            request.dispatcherType = DispatcherType.ASYNC
            request.isAsyncStarted = false
            request
        }
    }
}
