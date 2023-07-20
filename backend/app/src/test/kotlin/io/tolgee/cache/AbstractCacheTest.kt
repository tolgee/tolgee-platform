package io.tolgee.cache

import io.tolgee.AbstractSpringTest
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.constants.Caches
import io.tolgee.constants.MtServiceType
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractCacheTest : AbstractSpringTest() {
  @Autowired
  @MockBean
  lateinit var userAccountRepository: UserAccountRepository

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  @MockBean
  override lateinit var projectRepository: ProjectRepository

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  @MockBean
  lateinit var permissionRepository: PermissionRepository

  @Autowired
  @MockBean
  lateinit var googleTranslationProvider: GoogleTranslationProvider

  @Autowired
  @MockBean
  lateinit var awsTranslationProvider: AwsMtValueProvider

  private final val paramsEnGoogle by lazy {
    mtServiceManager.getParams(
      text = "Hello",
      textRaw = "raw-text",
      keyName = "key-name",
      sourceLanguageTag = "en",
      targetLanguageTag = "de",
      serviceType = MtServiceType.GOOGLE
    )
  }

  val paramsEnAws by lazy {
    paramsEnGoogle.copy(serviceType = MtServiceType.AWS)
  }

  @BeforeEach
  fun setup() {
    cacheManager.getCache(Caches.MACHINE_TRANSLATIONS)!!.clear()
    Mockito.clearInvocations(googleTranslationProvider, awsTranslationProvider)
  }

  @Test
  fun `caches user account`() {
    val user = UserAccount().apply {
      name = "Account"
      id = 10
    }
    whenever(userAccountRepository.findActive(user.id)).then { user }
    userAccountService.findDto(user.id)
    Mockito.verify(userAccountRepository, times(1)).findActive(user.id)
    userAccountService.findDto(user.id)
    Mockito.verify(userAccountRepository, times(1)).findActive(user.id)
  }

  @Test
  fun `caches project`() {
    val project = Project(id = 1)
    project.organizationOwner = Organization()
    whenever(projectRepository.findById(project.id)).then { Optional.of(project) }
    projectService.findDto(project.id)
    Mockito.verify(projectRepository, times(1)).findById(project.id)
    projectService.findDto(project.id)
    Mockito.verify(projectRepository, times(1)).findById(project.id)
  }

  @Test
  fun `caches permission by project and user`() {
    val permission = Permission(id = 1)
    whenever(permissionRepository.findOneByProjectIdAndUserIdAndOrganizationId(1, 1))
      .then { permission }
    permissionService.find(1, 1)
    Mockito.verify(permissionRepository, times(1))
      .findOneByProjectIdAndUserIdAndOrganizationId(1, 1)
    permissionService.find(1, 1)
    Mockito.verify(permissionRepository, times(1))
      .findOneByProjectIdAndUserIdAndOrganizationId(1, 1)
  }

  @Test
  fun `caches permission by organization`() {
    val permission = Permission(id = 1)
    whenever(
      permissionRepository
        .findOneByProjectIdAndUserIdAndOrganizationId(null, null, organizationId = 1)
    ).then { permission }

    permissionService.find(organizationId = 1)
    Mockito.verify(permissionRepository, times(1))
      .findOneByProjectIdAndUserIdAndOrganizationId(
        null,
        null,
        organizationId = 1
      )
    permissionService.find(organizationId = 1)
    Mockito.verify(permissionRepository, times(1))
      .findOneByProjectIdAndUserIdAndOrganizationId(
        null,
        null,
        1
      )
  }

  val googleResponse = MtValueProvider.MtResult("Hello", 10)

  @Test
  fun `is caching`() {
    cacheManager.getCache("cool cache")!!.put("test", "value")
    Assertions.assertThat(cacheManager.getCache("cool cache")!!.get("test")!!.get()).isEqualTo("value")
  }

  @Test
  fun `is caching machine translations`() {
    whenever(googleTranslationProvider.translate(any())).thenAnswer { googleResponse }
    mtServiceManager.translate(paramsEnGoogle)
    verify(googleTranslationProvider, times(1)).translate(any())
    mtServiceManager.translate(paramsEnGoogle)
    verify(googleTranslationProvider, times(1)).translate(any())
  }

  @Test
  fun `is not caching machine translations (different service)`() {
    whenever(googleTranslationProvider.translate(any())).thenAnswer { googleResponse }
    mtServiceManager.translate(paramsEnGoogle)
    verify(googleTranslationProvider, times(1)).translate(any())
    mtServiceManager.translate(paramsEnAws)
    verify(awsTranslationProvider, times(1)).translate(any())
  }

  @Test
  fun `is not caching machine translations (different targetLang)`() {
    whenever(googleTranslationProvider.translate(any())).thenAnswer { googleResponse }
    mtServiceManager.translate(paramsEnGoogle)
    verify(googleTranslationProvider, times(1)).translate(any())
    mtServiceManager.translate(paramsEnGoogle.copy(targetLanguageTag = "cs"))
    verify(googleTranslationProvider, times(2)).translate(any())
  }

  @Test
  fun `is not caching machine translations (different sourceLang)`() {
    whenever(googleTranslationProvider.translate(any())).thenAnswer { googleResponse }
    mtServiceManager.translate(paramsEnGoogle)
    verify(googleTranslationProvider, times(1)).translate(any())
    mtServiceManager.translate(paramsEnGoogle.copy(sourceLanguageTag = "de"))
    verify(googleTranslationProvider, times(2)).translate(any())
  }

  @Test
  fun `is not caching machine translations (different input)`() {
    whenever(googleTranslationProvider.translate(any())).thenAnswer { googleResponse }
    mtServiceManager.translate(paramsEnGoogle)
    verify(googleTranslationProvider, times(1)).translate(any())
    mtServiceManager.translate(paramsEnGoogle.copy(text = "Hello!"))
    verify(googleTranslationProvider, times(2)).translate(any())
  }
}
