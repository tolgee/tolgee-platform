package io.tolgee

import io.tolgee.constants.Message
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.Pat
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import java.util.Date

class PatAuthTest : AbstractControllerTest() {
  @Test
  fun `user authorizes with PAT`() {
    val pat = createUserWithPat()

    performGet(
      "/v2/user",
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_${pat.token}")
      },
    ).andIsOk.andAssertThatJson {
      node("username").isEqualTo("franta")
    }

    val lastUsedAt = patService.get(pat.id).lastUsedAt
    assertThat(lastUsedAt).isNotNull
    assertThat(lastUsedAt?.time).isLessThan(Date().time)
  }

  @Test
  fun `throws project not selected`() {
    val pat = createUserWithPat()

    performGet(
      "/v2/projects/translations/en",
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_${pat.token}")
      },
    ).andIsBadRequest.andHasErrorMessage(Message.PROJECT_NOT_SELECTED)
  }

  @Test
  fun `user authorizes with PAT with no expiration`() {
    val pat = createUserWithPat(null)

    performGet(
      "/v2/user",
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_${pat.token}")
      },
    ).andIsOk
  }

  @Test
  fun `user doesnt authorize with wrong PAT`() {
    performGet(
      "/v2/user",
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_nopat")
      },
    ).andIsUnauthorized
  }

  @Test
  fun `user doesnt authorize with expired PAT`() {
    val pat = createUserWithPat(expiresAt = Date(Date().time - 10000))
    performGet(
      "/v2/user",
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_${pat.token}")
      },
    ).andIsUnauthorized
  }

  @Test
  fun `pat doesnt work on restricted endpoint`() {
    val pat = createUserWithPat(expiresAt = Date(Date().time + 100000))
    performDelete(
      "/v2/pats/${pat.id}",
      content = null,
      httpHeaders =
        HttpHeaders().apply {
          add("X-API-Key", "tgpat_${pat.token}")
        },
    ).andIsForbidden
  }

  private fun createUserWithPat(expiresAt: Date? = Date(Date().time + 10000)): Pat {
    var pat: Pat? = null
    testDataService.saveTestData {
      addUserAccount {
        username = "franta"
      }.build {
        addPat {
          description = "My cool pat..."
          this.expiresAt = expiresAt
          pat = this
        }
      }
    }
    return pat!!
  }
}
