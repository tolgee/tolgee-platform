package io.tolgee

import com.google.cloud.translate.Translate
import com.posthog.java.PostHog
import io.tolgee.component.MaxUploadedFilesByUserProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.AzureCognitiveApiService
import io.tolgee.component.machineTranslation.providers.BaiduApiService
import io.tolgee.component.machineTranslation.providers.DeeplApiService
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.component.machineTranslation.providers.TolgeeTranslateApiService
import io.tolgee.component.mtBucketSizeProvider.MtBucketSizeProvider
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.organization.OrganizationService
import io.tolgee.testing.mocking.MockWrappedBean
import org.mockito.AdditionalAnswers
import org.mockito.Mockito
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import sibApi.ContactsApi
import software.amazon.awssdk.services.translate.TranslateClient

class ServerAppTestOverridesConfiguration {
  @MockWrappedBean
  @Bean
  @Primary
  fun googleTranslateMock(
    real: Translate
  ): Translate {
    return Mockito.mock(Translate::class.java)
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun amazonTranslateMock(
    real: TranslateClient
  ): TranslateClient {
    return Mockito.mock(TranslateClient::class.java)
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun deeplApiServiceMock(
    real: DeeplApiService
  ): DeeplApiService {
    return Mockito.mock(DeeplApiService::class.java)
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun azureCognitiveServiceMock(
    real: AzureCognitiveApiService
  ): AzureCognitiveApiService {
    return Mockito.mock(AzureCognitiveApiService::class.java)
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun baiduApiServiceMock(
    real: BaiduApiService
  ): BaiduApiService {
    return Mockito.mock(BaiduApiService::class.java)
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun tolgeeTranslateApiServiceMock(
    real: TolgeeTranslateApiService
  ): TolgeeTranslateApiService {
    return Mockito.mock(TolgeeTranslateApiService::class.java)
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun cacheManagerMock(
    real: CacheManager
  ): CacheManager {
    return Mockito.mock(CacheManager::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun postHogMock(
    real: PostHog?
  ): PostHog {
    return Mockito.mock(PostHog::class.java)
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun mtBucketSizeProviderMock(
    real: MtBucketSizeProvider
  ): MtBucketSizeProvider {
    return Mockito.mock(MtBucketSizeProvider::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun maxUploadedFilesByUserProviderMock(
    real: MaxUploadedFilesByUserProvider
  ): MaxUploadedFilesByUserProvider {
    return Mockito.mock(MaxUploadedFilesByUserProvider::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun contentDeliveryCachePurgingProviderMock(
    real: ContentDeliveryCachePurgingProvider
  ): ContentDeliveryCachePurgingProvider {
    return Mockito.mock(ContentDeliveryCachePurgingProvider::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun organizationServiceMock(
    real: OrganizationService
  ): OrganizationService {
    return Mockito.mock(OrganizationService::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun userAccountRepositoryMock(
    real: UserAccountRepository
  ): UserAccountRepository {
    return Mockito.mock(UserAccountRepository::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun projectRepositoryMock(
    real: ProjectRepository
  ): ProjectRepository {
    return Mockito.mock(ProjectRepository::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun permissionRepositoryMock(
    real: PermissionRepository
  ): PermissionRepository {
    return Mockito.mock(PermissionRepository::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun googleTranslationProviderMock(
    real: GoogleTranslationProvider
  ): GoogleTranslationProvider {
    return Mockito.mock(GoogleTranslationProvider::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun awsTranslationProviderMock(
    real: AwsMtValueProvider
  ): AwsMtValueProvider {
    return Mockito.mock(AwsMtValueProvider::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun contactsApiMock(
    real: ContactsApi?
  ): ContactsApi {
    return Mockito.mock(ContactsApi::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }
}
