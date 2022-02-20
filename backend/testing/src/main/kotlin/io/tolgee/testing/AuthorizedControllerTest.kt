package io.tolgee.testing

import io.tolgee.fixtures.AuthRequestPerformer
import io.tolgee.fixtures.LoggedRequestFactory.init
import io.tolgee.fixtures.SignedInRequestPerformer
import io.tolgee.model.UserAccount
import io.tolgee.security.JwtTokenProvider
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
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
  lateinit var signedInRequestPerformer: SignedInRequestPerformer

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
    _userAccount = userAccountService.findOptional(userName).orElseGet {
      dbPopulator.createUserIfNotExists("admin")
    }
    init(jwtTokenProvider.generateToken(_userAccount!!.id).toString())
  }

  fun loginAsUser(userAccount: UserAccount) {
    _userAccount = userAccount
    init(jwtTokenProvider.generateToken(_userAccount!!.id).toString())
  }

  fun logout() {
    _userAccount = null
  }

  override fun perform(builder: MockHttpServletRequestBuilder): ResultActions {
    return requestPerformer.perform(builder)
  }

  override fun performDelete(url: String, content: Any?): ResultActions {
    return requestPerformer.performDelete(url, content)
  }

  override fun performGet(url: String): ResultActions {
    return requestPerformer.performGet(url)
  }

  override fun performPost(url: String, content: Any?): ResultActions {
    return requestPerformer.performPost(url, content)
  }

  override fun performPut(url: String, content: Any?): ResultActions {
    return requestPerformer.performPut(url, content)
  }

  override fun performAuthPut(url: String, content: Any?): ResultActions {
    loginAsAdminIfNotLogged()
    return signedInRequestPerformer.performAuthPut(url, content)
  }

  override fun performAuthPost(url: String, content: Any?): ResultActions {
    loginAsAdminIfNotLogged()
    return signedInRequestPerformer.performAuthPost(url, content)
  }

  override fun performAuthGet(url: String): ResultActions {
    loginAsAdminIfNotLogged()
    return signedInRequestPerformer.performAuthGet(url)
  }

  override fun performAuthDelete(url: String, content: Any?): ResultActions {
    loginAsAdminIfNotLogged()
    return signedInRequestPerformer.performAuthDelete(url, content)
  }

  override fun performAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>>
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return signedInRequestPerformer.performAuthMultipart(url, files, params)
  }
}
