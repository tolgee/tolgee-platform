package io.tolgee.testing

import io.tolgee.fixtures.AuthRequestPerformer
import io.tolgee.fixtures.AuthorizedRequestFactory.init
import io.tolgee.fixtures.AuthorizedRequestPerformer
import io.tolgee.model.UserAccount
import io.tolgee.security.JwtTokenProvider
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

abstract class AuthorizedControllerTest : AbstractControllerTest(), AuthRequestPerformer {
  private var _userAccount: UserAccount? = null

  var userAccount: UserAccount?
    get() {
      if (_userAccount == null) {
        // populate to create the user if not created
        dbPopulator.createUserIfNotExists(tolgeeProperties.authentication.initialUsername)
        loginAsUser(tolgeeProperties.authentication.initialUsername)
      }
      return _userAccount
    }
    set(userAccount) {
      if (userAccount != null) {
        loginAsUser(userAccount)
      }
      _userAccount = userAccount
    }

  @Autowired
  lateinit var authorizedRequestPerformer: AuthorizedRequestPerformer

  @Autowired
  lateinit var jwtTokenProvider: JwtTokenProvider

  @AfterEach
  fun afterEach() {
    logout()
  }

  fun loginAsAdminIfNotLogged() {
    if (_userAccount == null) {
      loginAsUser("admin")
    }
  }

  fun loginAsUser(userName: String) {
    val account = userAccountService.findActive(userName) ?: dbPopulator.createUserIfNotExists("admin")
    loginAsUser(account)
  }

  fun loginAsUser(userAccount: UserAccount) {
    _userAccount = userAccount
    init(generateJwtToken(_userAccount!!.id))
  }

  protected fun generateJwtToken(userAccountId: Long) = jwtTokenProvider.generateToken(userAccountId, true).toString()

  fun refreshUser() {
    _userAccount = userAccountService.findActive(_userAccount!!.id)
  }

  fun logout() {
    _userAccount = null
  }

  override fun perform(builder: MockHttpServletRequestBuilder): ResultActions {
    return requestPerformer.perform(builder)
  }

  override fun performDelete(url: String, content: Any?, httpHeaders: HttpHeaders): ResultActions {
    return requestPerformer.performDelete(url, content)
  }

  override fun performGet(url: String, httpHeaders: HttpHeaders): ResultActions {
    return requestPerformer.performGet(url, httpHeaders)
  }

  override fun performPost(url: String, content: Any?, httpHeaders: HttpHeaders): ResultActions {
    return requestPerformer.performPost(url, content, httpHeaders)
  }

  override fun performPut(url: String, content: Any?, httpHeaders: HttpHeaders): ResultActions {
    return requestPerformer.performPut(url, content, httpHeaders)
  }

  override fun performAuthPut(url: String, content: Any?): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthPut(url, content)
  }

  override fun performAuthPost(url: String, content: Any?): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthPost(url, content)
  }

  override fun performAuthGet(url: String): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthGet(url)
  }

  override fun performAuthDelete(url: String, content: Any?): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthDelete(url, content)
  }

  override fun performAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>>
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthMultipart(url, files, params)
  }
}
