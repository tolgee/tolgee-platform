/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ScreenshotController

import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.satisfies
import io.tolgee.model.Permission
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.authentication.secured-image-retrieval=true",
    "tolgee.authentication.secured-image-timestamp-max-age=10000",
  ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecuredKeyScreenshotControllerTest : AbstractV2ScreenshotControllerTest() {
  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  @Test
  fun getScreenshotFileNoTimestamp() {
    executeInNewTransaction {
      val base = dbPopulator.createBase()
      val project = base.project
      val key = keyService.create(project, CreateKeyDto("test"))
      val screenshot = screenshotService.store(screenshotFile, key, null)

      performGet("/screenshots/${screenshot.filename}").andIsNotFound
    }
  }

  @Test
  fun getScreenshotFileInvalidTimestamp() {
    executeInNewTransaction {
      val base = dbPopulator.createBase()
      val project = base.project
      val key = keyService.create(project, CreateKeyDto("test"))
      val screenshot = screenshotService.store(screenshotFile, key, null)

      val token =
        jwtService.emitTicket(
          userAccount!!.id,
          JwtService.TicketType.IMG_ACCESS,
          5000,
          mapOf(
            "fileName" to screenshot.filename,
            "projectId" to project.id.toString(),
          ),
        )

      moveCurrentDate(Duration.ofSeconds(10))
      performGet("/screenshots/${screenshot.filename}?token=$token").andIsUnauthorized
    }
  }

  // Renamed from getScreenshotFile to avoid Spring bean introspection conflict
  @Test
  fun performGetScreenshotFile() {
    executeInNewTransaction {
      val base = dbPopulator.createBase()
      val project = base.project
      val key = keyService.create(project, CreateKeyDto("test"))
      val screenshot = screenshotService.store(screenshotFile, key, null)

      val token =
        jwtService.emitTicket(
          userAccount!!.id,
          JwtService.TicketType.IMG_ACCESS,
          5000,
          mapOf(
            "fileName" to screenshot.filename,
            "projectId" to project.id.toString(),
          ),
        )

      performGet("/screenshots/${screenshot.filename}?token=$token").andIsOk
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun uploadScreenshot() {
    val key = keyService.create(project, CreateKeyDto("test"))

    performStoreScreenshot(project, key).andIsCreated.andAssertThatJson {
      executeInNewTransaction {
        val screenshots = screenshotService.findAll(key = key)
        assertThat(screenshots).hasSize(1)
        val bytes = fileStorage.readFile("screenshots/" + screenshots[0].filename)
        assertThat(bytes.size).isCloseTo(1070, Offset.offset(500))
        node("filename").isString.startsWith(screenshots[0].filename).satisfies {
          val parts = it.split("?token=")
          val auth = jwtService.validateTicket(parts[1], JwtService.TicketType.IMG_ACCESS)
          assertThat(auth.data?.get("fileName")).isEqualTo(parts[0])
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun findAll() {
    executeInNewTransaction {
      val key = keyService.create(project, CreateKeyDto("test"))
      screenshotService.store(screenshotFile, key, null)
      performProjectAuthGet("/keys/${key.id}/screenshots").andIsOk.andAssertThatJson {
        node("_embedded.screenshots[0].filename").isString.satisfies {
          val parts = it.split("?token=")
          val auth = jwtService.validateTicket(parts[1], JwtService.TicketType.IMG_ACCESS)
          assertThat(auth.data?.get("fileName")).isEqualTo(parts[0])
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it applies project permissions when accessing the screenshot`() {
    executeInNewTransaction {
      val newUser = dbPopulator.createUserIfNotExists("screenshot-test-user-1")
      val newUserRandom = dbPopulator.createUserIfNotExists("screenshot-test-user-2")

      val userPermission =
        permissionService.create(
          Permission(
            type = null,
            user = newUser,
            project = project,
            scopes = Scope.expand(Scope.SCREENSHOTS_UPLOAD),
          ),
        )

      val key = keyService.create(project, CreateKeyDto("test"))
      val screenshot = screenshotService.store(screenshotFile, key, null)

      val token =
        jwtService.emitTicket(
          newUser.id,
          JwtService.TicketType.IMG_ACCESS,
          5000,
          mapOf(
            "fileName" to screenshot.filename,
            "projectId" to project.id.toString(),
          ),
        )

      // Login as someone who has 0 access to the project and see if it works
      loginAsUser(newUserRandom)

      performGet("/screenshots/${screenshot.filename}?token=$token").andIsOk
      permissionService.delete(userPermission)
      performGet("/screenshots/${screenshot.filename}?token=$token").andIsNotFound
    }
  }
}
