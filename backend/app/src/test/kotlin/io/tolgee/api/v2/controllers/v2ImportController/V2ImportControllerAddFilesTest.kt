package io.tolgee.api.v2.controllers.v2ImportController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessage
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_ARCHIVE
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_FILES_IN_ARCHIVE
import io.tolgee.fixtures.LoggedRequestFactory
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Project
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.transaction.annotation.Transactional
import javax.servlet.DispatcherType

@Transactional
class V2ImportControllerAddFilesTest : AuthorizedControllerTest() {
  @Value("classpath:import/zipOfJsons.zip")
  lateinit var zipOfJsons: Resource

  @Value("classpath:import/zipOfUnknown.zip")
  lateinit var zipOfUnknown: Resource

  @Value("classpath:import/error.json")
  lateinit var errorJson: Resource

  @Value("classpath:import/po/example.po")
  lateinit var poFile: Resource

  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  @Value("classpath:import/xliff/example.xliff")
  lateinit var xliffFile: Resource

  @Value("classpath:import/tooLongTranslation.json")
  lateinit var tooLongTranslation: Resource

  @Value("classpath:import/tooLongErrorParamValue.json")
  lateinit var tooLongErrorParamValue: Resource

  @AfterEach
  fun resetProps() {
    tolgeeProperties.maxTranslationTextLength = 10000
  }

  @Test
  fun `it parses zip file and saves issues`() {
    val project = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    performStreamingImport(projectId = project.id, mapOf(Pair("zipOfUnknown.zip", zipOfUnknown)))
      .andAssertContainsMessage(FOUND_FILES_IN_ARCHIVE, listOf(3))
      .andAssertContainsMessage(FOUND_ARCHIVE).andPrettyPrintStreamingResult().andAssertStreamingResultJson {
        node("errors[2].code").isEqualTo("cannot_parse_file")
      }
  }

  @Test
  fun `it saves proper data and returns correct response `() {
    val project = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    performImport(projectId = project.id, mapOf(Pair("zipOfJsons.zip", zipOfJsons)))
      .andPrettyPrint.andAssertThatJson {
        node("result._embedded.languages").isArray.hasSize(3)
      }
    validateSavedJsonImportData(project)
  }

  @Test
  fun `it handles po file`() {
    val project = dbPopulator.createBase(generateUniqueString())

    performImport(projectId = project.id, mapOf(Pair("example.po", poFile)))
      .andPrettyPrint.andAssertThatJson {
        node("result._embedded.languages").isArray.hasSize(1)
      }.andReturn()

    entityManager.clear()

    importService.find(project.id, project.userOwner?.id!!)?.let {
      assertThat(it.files).hasSize(1)
      assertThat(it.files[0].languages[0].translations).hasSize(8)
    }
  }

  @Test
  fun `it handles xliff file`() {
    val project = dbPopulator.createBase(generateUniqueString())

    performImport(projectId = project.id, mapOf(Pair("example.xliff", xliffFile)))
      .andPrettyPrint.andAssertThatJson {
        node("result._embedded.languages").isArray.hasSize(2)
      }.andReturn()
  }

  @Test
  fun `it returns error when json could not be parsed`() {
    val project = dbPopulator.createBase(generateUniqueString())

    performImport(projectId = project.id, mapOf(Pair("error.json", errorJson)))
      .andIsOk.andAssertThatJson {
        node("errors[0].code").isEqualTo("cannot_parse_file")
        node("errors[0].params[0]").isEqualTo("error.json")
        node("errors[0].params[1]").isString.contains("Unrecognized token")
      }
  }

  @Test
  fun `it throws when more then 100 languages`() {
    val project = dbPopulator.createBase(generateUniqueString())

    val data = (1..101).associate { "simple$it.json" as String? to simpleJson }

    performImport(projectId = project.id, data)
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("code").isEqualTo("cannot_add_more_then_100_languages")
      }
  }

  @Test
  fun `it saves proper data and returns correct response (streamed)`() {
    val project = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    performStreamingImport(projectId = project.id, mapOf(Pair("zipOfJsons.zip", zipOfJsons)))
      .andAssertContainsMessage(FOUND_FILES_IN_ARCHIVE, listOf(3))
      .andAssertContainsMessage(FOUND_ARCHIVE).andPrettyPrintStreamingResult().andAssertStreamingResultJson {
        node("result._embedded.languages").isArray.hasSize(3)
      }
    validateSavedJsonImportData(project)
  }

  @Test
  fun `it adds a file with long translation text and stores issues`() {
    val project = dbPopulator.createBase(generateUniqueString())
    commitTransaction()
    tolgeeProperties.maxTranslationTextLength = 20

    performStreamingImport(projectId = project.id, mapOf(Pair("tooLongTranslation.json", tooLongTranslation))).andIsOk

    importService.find(project.id, project.userOwner?.id!!)?.let {
      assertThat(it.files).hasSize(1)
      assertThat(it.files[0].issues).hasSize(1)
      assertThat(it.files[0].issues[0].type).isEqualTo(FileIssueType.TRANSLATION_TOO_LONG)
      assertThat(it.files[0].issues[0].params?.get(0)?.value).isEqualTo("too_long")
    }
  }

  @Test
  fun `stores issue with too long value`() {
    val project = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    performStreamingImport(
      projectId = project.id,
      mapOf(Pair("tooLongErrorParamValue.json", tooLongErrorParamValue))
    ).andIsOk

    importService.find(project.id, project.userOwner?.id!!)!!.let {
      assertThat(it.files[0].issues[0].params?.get(0)?.value).isEqualTo("not_string")
      assertThat(it.files[0].issues[0].params?.get(2)?.value).isEqualTo(
        "[Lorem ipsum dolor sit amet," +
          " consectetur adipiscing elit. Suspendisse" +
          " ac ultricies tortor. Integer ac..."
      )
    }
  }

  private fun validateSavedJsonImportData(project: Project) {
    importService.find(project.id, project.userOwner?.id!!)!!.let { importEntity ->
      entityManager.refresh(importEntity)
      assertThat(importEntity.files.size).isEqualTo(3)
      assertThat(importEntity.files.map { it.name }).containsAll(listOf("en.json", "cs.json", "fr.json"))
      val keys = importService.findKeys(importEntity)
      keys.forEach { key ->
        assertThat(keys.filter { it.name == key.name })
          .describedAs("Each key is stored just once")
          .hasSizeLessThan(2)
      }
      importEntity.files.forEach {
        assertThat(it.issues).hasSize(0)
      }
      assertThat(keys).hasSize(206)
    }
  }

  private fun performImport(projectId: Long, files: Map<String?, Resource>): ResultActions {
    val builder = MockMvcRequestBuilders
      .multipart("/v2/projects/$projectId/import")

    files.forEach {
      builder.file(
        MockMultipartFile(
          "files", it.key, "application/zip",
          it.value.file.readBytes()
        )
      )
    }

    loginAsUser("admin")
    return mvc.perform(LoggedRequestFactory.addToken(builder))
  }

  private fun performStreamingImport(projectId: Long, files: Map<String?, Resource>): ResultActions {
    val builder = MockMvcRequestBuilders
      .multipart("/v2/projects/$projectId/import/with-streaming-response")

    files.forEach {
      builder.file(
        MockMultipartFile(
          "files", it.key, "application/zip",
          it.value.file.readBytes()
        )
      )
    }

    loginAsUser("admin")
    mvc.perform(
      LoggedRequestFactory.addToken(
        builder
      )
    ).andReturn().let {
      return mvc.perform(asyncDispatch(it))
    }
  }

  private fun ResultActions.andAssertContainsMessage(
    type: ImportStreamingProgressMessageType,
    params: List<Any?>? = null
  ): ResultActions {
    this.andReturn().response.contentAsString.removeSuffix(";;;").split(";;;").any {
      val mapped = jacksonObjectMapper().readValue<ImportStreamingProgressMessage>(it)
      ImportStreamingProgressMessage(type, params) == mapped
    }.let {
      assertThat(it).describedAs(
        """Streaming response contains message of type $type 
                |with params $params
            """.trimMargin()
      ).isTrue
    }
    return this
  }

  private fun ResultActions.andAssertStreamingResultJson(
    jsonAssert: JsonAssert.ConfigurableJsonAssert.() -> Unit
  ): ResultActions {
    val rawResult = this.andReturn().response.contentAsString
      .removeSuffix(";;;").split(";;;").last()
    jsonAssert(assertThatJson(rawResult))
    return this
  }

  private fun ResultActions.andPrettyPrintStreamingResult(): ResultActions {
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
