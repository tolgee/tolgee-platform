package io.tolgee.testing

import io.tolgee.development.testDataBuilder.ContributorActivityRecorder
import io.tolgee.fixtures.AuthRequestPerformer
import io.tolgee.fixtures.AuthorizedRequestFactory.init
import io.tolgee.fixtures.AuthorizedRequestPerformer
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.UserSessionType
import io.tolgee.security.authentication.JwtService
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import java.time.Duration
import java.util.Date
import java.util.UUID

abstract class AuthorizedControllerTest :
  AbstractControllerTest(),
  AuthRequestPerformer {
  private var _userAccount: UserAccount? = null

  private var loginDeviceId: String? = null

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
  lateinit var jwtService: JwtService

  @AfterEach
  fun afterEach() {
    logout()
  }

  fun loginAsAdminIfNotLogged() {
    if (_userAccount == null) {
      loginAsUser(initialUsername)
    }
  }

  fun loginAsUserIfNotLogged() {
    if (_userAccount == null) {
      loginAsUser("_test_user")
    }
  }

  fun loginAsUser(userName: String) {
    val account = userAccountService.findActive(userName) ?: dbPopulator.createUserIfNotExists(userName)
    loginAsUser(account)
  }

  fun loginAsUser(userAccount: UserAccount) {
    _userAccount = userAccount
    loginDeviceId = UUID.randomUUID().toString()
    init(generateJwtToken(_userAccount!!.id))
  }

  /**
   * Reuses the device of the current login so that moving the clock refreshes that session instead
   * of piling up a new one on every move.
   */
  protected fun generateJwtToken(userAccountId: Long) =
    jwtService.emitToken(
      userAccountId,
      type = UserSessionType.TEST,
      isSuper = true,
      refreshedDeviceId = loginDeviceId,
      isRefresh = loginDeviceId != null,
    )

  protected fun setSecurityContext(userAccount: UserAccount) {
    val token = jwtService.emitToken(userAccount.id, type = UserSessionType.TEST, isSuper = true)
    val auth = jwtService.validateToken(token)
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = auth
    SecurityContextHolder.setContext(context)
  }

  fun refreshUser() {
    _userAccount = userAccountService.findActive(_userAccount!!.id)
  }

  fun logout() {
    _userAccount = null
    loginDeviceId = null
    SecurityContextHolder.clearContext()
  }

  override fun perform(builder: MockHttpServletRequestBuilder): ResultActions {
    return requestPerformer.perform(builder)
  }

  override fun performDelete(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performDelete(url, content)
  }

  override fun performGet(
    url: String,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performGet(url, httpHeaders)
  }

  override fun performPost(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performPost(url, content, httpHeaders)
  }

  override fun performPut(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performPut(url, content, httpHeaders)
  }

  override fun performAuthPut(
    url: String,
    content: Any?,
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthPut(url, content)
  }

  override fun performAuthPost(
    url: String,
    content: Any?,
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthPost(url, content)
  }

  override fun performAuthGet(url: String): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthGet(url)
  }

  override fun performAuthDelete(
    url: String,
    content: Any?,
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthDelete(url, content)
  }

  override fun performAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>>,
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return authorizedRequestPerformer.performAuthMultipart(url, files, params)
  }

  fun refreshJwtToken() {
    if (_userAccount != null) {
      init(generateJwtToken(_userAccount!!.id))
    }
  }

  override fun setForcedDate(date: Date) {
    super.setForcedDate(date)
    refreshJwtToken()
  }

  override fun clearForcedDate() {
    super.clearForcedDate()
    refreshJwtToken()
  }

  override fun forceDateString(
    dateString: String,
    pattern: String,
  ) {
    super.forceDateString(dateString, pattern)
    refreshJwtToken()
  }

  override fun moveCurrentDate(duration: Duration) {
    super.moveCurrentDate(duration)
    refreshJwtToken()
  }

  protected fun recordProjectActivity(
    projectId: Long?,
    authorId: Long?,
    at: Date? = null,
  ) {
    executeInNewTransaction {
      ContributorActivityRecorder.record(entityManager, currentDateProvider, projectId, authorId, at)
    }
  }
}
