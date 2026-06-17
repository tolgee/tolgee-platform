package io.tolgee.api.v2.controllers

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.component.MaxUploadedFilesByUserProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.generateImage
import io.tolgee.fixtures.undecodableImageBytes
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.transaction.PlatformTransactionManager
import java.net.URI
import java.time.Duration

@AutoConfigureMockMvc
class PublicImageUploadControllerTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData

  @Autowired lateinit var mockMvc: MockMvc

  @Autowired lateinit var jwtService: JwtService

  @Autowired lateinit var transactionManager: PlatformTransactionManager

  @MockitoBean
  @Autowired
  lateinit var maxUploadedFilesByUserProvider: MaxUploadedFilesByUserProvider

  @BeforeEach
  fun setup() {
    data = createTestDataWithPak()
    whenever(maxUploadedFilesByUserProvider.invoke()).thenAnswer { 100L }
  }

  @Test
  fun `get_image_upload_url returns a well-formed URL with a valid IMG_UPLOAD token`() {
    val client = mcpClient()
    val json = callToolAndGetJson(client, "get_image_upload_url")
    val uri = URI(json["uploadUrl"].asText())
    assertThat(uri.path).isEqualTo("/v2/public/image-upload")
    assertThat(json["expiresInSeconds"].asLong()).isEqualTo(1800)
    val auth = jwtService.validateTicket(tokenOf(uri), JwtService.TicketType.IMG_UPLOAD)
    assertThat(auth.userAccount.id).isEqualTo(data.userAccountId)
  }

  @Test
  fun `end-to-end - tool URL, upload, then use the id`() {
    val client = mcpClient()
    val uri = URI(callToolAndGetJson(client, "get_image_upload_url")["uploadUrl"].asText())

    val result = upload(tokenOf(uri), imageFile()).andIsCreated.andReturn()
    val id = objectMapper.readTree(result.response.contentAsString)["uploadedImageId"].asLong()
    assertThat(id).isGreaterThan(0)

    executeInNewTransaction(transactionManager) {
      val image = imageUploadService.find(listOf(id)).first()
      assertThat(image.userAccount.id).isEqualTo(data.userAccountId)
      assertThat(fileStorage.fileExists("uploadedImages/" + image.filenameWithExtension)).isTrue()
    }

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "e2e.key", "translations" to mapOf("en" to "x"))),
      ),
    )
    val assoc =
      callToolAndGetJson(
        client,
        "add_key_screenshots",
        mapOf(
          "projectId" to data.projectId,
          "keyScreenshots" to
            listOf(mapOf("keyName" to "e2e.key", "screenshots" to listOf(mapOf("uploadedImageId" to id)))),
        ),
      )
    assertThat(assoc["success"].asBoolean()).isTrue()
  }

  @Test
  fun `rejects an expired token`() {
    val token = uploadToken()
    moveCurrentDate(Duration.ofMinutes(6))
    upload(token, imageFile()).andIsUnauthorized
  }

  @Test
  fun `rejects a garbage token`() {
    upload("not-a-real-token", imageFile()).andIsUnauthorized
  }

  @Test
  fun `rejects a missing token`() {
    mockMvc.perform(multipart("/v2/public/image-upload").file(imageFile())).andIsBadRequest
  }

  @Test
  fun `rejects a token of the wrong ticket type`() {
    val token = jwtService.emitTicket(data.userAccountId, JwtService.TicketType.IMG_ACCESS)
    upload(token, imageFile()).andIsUnauthorized
  }

  @Test
  fun `rejects a token for a non-existent user (fail-closed)`() {
    val token = jwtService.emitTicket(999_999_999L, JwtService.TicketType.IMG_UPLOAD)
    upload(token, imageFile()).andIsUnauthorized
  }

  @Test
  fun `rejects a token after the user is deactivated (fail-closed)`() {
    val token = uploadToken()
    userAccountService.disable(data.userAccountId)
    upload(token, imageFile()).andIsUnauthorized
  }

  @Test
  fun `rejects a part with a missing content-type as FILE_NOT_IMAGE`() {
    val file = MockMultipartFile("image", "shot.jpg", null, generateImage(80, 80).inputStream.readBytes())
    upload(uploadToken(), file).andIsBadRequest.andAssertThatJson {
      node("CUSTOM_VALIDATION.file_not_image").isArray
    }
  }

  @Test
  fun `rejects a non-image payload with FILE_NOT_IMAGE`() {
    val file = MockMultipartFile("image", "note.txt", "text/plain", "hello".toByteArray())
    upload(uploadToken(), file).andIsBadRequest.andAssertThatJson {
      node("CUSTOM_VALIDATION.file_not_image").isArray
    }
  }

  @Test
  fun `rejects undecodable bytes with a valid content-type as FILE_NOT_IMAGE, not 500`() {
    val file = MockMultipartFile("image", "broken.png", "image/png", undecodableImageBytes)
    upload(uploadToken(), file).andIsBadRequest.andAssertThatJson {
      node("CUSTOM_VALIDATION.file_not_image").isArray
    }
  }

  @Test
  fun `accepts a degenerate aspect-ratio image (no 500 from a zero dimension)`() {
    upload(uploadToken(), imageFile(201, 1)).andIsCreated
    upload(uploadToken(), imageFile(1, 201)).andIsCreated
  }

  @Test
  fun `rejects when the per-user upload quota is exceeded`() {
    whenever(maxUploadedFilesByUserProvider.invoke()).thenAnswer { 0L }
    upload(uploadToken(), imageFile()).andIsCreated
    upload(uploadToken(), imageFile()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("too_many_uploaded_images")
    }
  }

  @Test
  fun `the URL is reusable within its expiry window`() {
    val token = uploadToken()
    val first =
      objectMapper.readTree(
        upload(token, imageFile())
          .andIsCreated
          .andReturn()
          .response.contentAsString,
      )
    val second =
      objectMapper.readTree(
        upload(token, imageFile())
          .andIsCreated
          .andReturn()
          .response.contentAsString,
      )
    val firstId = first["uploadedImageId"].asLong()
    val secondId = second["uploadedImageId"].asLong()
    assertThat(firstId).isGreaterThan(0)
    assertThat(secondId).isGreaterThan(0).isNotEqualTo(firstId)

    executeInNewTransaction(transactionManager) {
      imageUploadService.find(listOf(firstId, secondId)).forEach {
        assertThat(it.userAccount.id).isEqualTo(data.userAccountId)
      }
    }
  }

  private fun mcpClient(): McpSyncClient = createMcpClientWithPak(data.apiKey.encodedKey!!)

  private fun uploadToken(userId: Long = data.userAccountId): String =
    jwtService.emitTicket(userId, JwtService.TicketType.IMG_UPLOAD)

  private fun tokenOf(uri: URI): String = uri.query.substringAfter("token=")

  private fun imageFile(
    width: Int = 80,
    height: Int = 80,
  ): MockMultipartFile = MockMultipartFile("image", "shot.jpg", "image/jpeg", generateImage(width, height).inputStream)

  private fun upload(
    token: String,
    file: MockMultipartFile,
  ): ResultActions = mockMvc.perform(multipart("/v2/public/image-upload").file(file).param("token", token))
}
