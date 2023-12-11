package io.tolgee

import com.google.cloud.translate.Translate
import com.posthog.java.PostHog
import io.tolgee.batch.BatchJobActivityFinalizer
import io.tolgee.batch.BatchJobCancellationManager
import io.tolgee.batch.BatchJobProjectLockingManager
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.processors.AutomationChunkProcessor
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.MachineTranslationChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.MaxUploadedFilesByUserProvider
import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.component.machineTranslation.providers.AzureCognitiveApiService
import io.tolgee.component.machineTranslation.providers.BaiduApiService
import io.tolgee.component.machineTranslation.providers.DeeplApiService
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.component.machineTranslation.providers.TolgeeTranslateApiService
import io.tolgee.component.mtBucketSizeProvider.MtBucketSizeProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.testing.mocking.MockWrappedBean
import jakarta.persistence.EntityManager
import org.mockito.AdditionalAnswers
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import sibApi.ContactsApi
import software.amazon.awssdk.services.translate.TranslateClient

// For some reason, Spring Fails to initialize mocked classes when we use web environment with random port
// So for such tests, we need to fallback to good old @MockBean
@ConditionalOnProperty("is-test-with-random-port", havingValue = "false", matchIfMissing = true)
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

  @MockWrappedBean
  @Bean
  @Primary
  fun contentDeliveryFileStorageProviderMock(
    real: ContentDeliveryFileStorageProvider
  ): ContentDeliveryFileStorageProvider {
    return Mockito.mock(ContentDeliveryFileStorageProvider::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun tolgeePropertiesMock(
    real: TolgeeProperties
  ): TolgeeProperties {
    return Mockito.mock(TolgeeProperties::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun autoTranslationServiceMock(
    real: AutoTranslationService
  ): AutoTranslationService {
    return Mockito.mock(AutoTranslationService::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun batchJobActivityFinalizerMock(
    real: BatchJobActivityFinalizer
  ): BatchJobActivityFinalizer {
    return Mockito.mock(BatchJobActivityFinalizer::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun mtCreditBucketServiceMock(
    real: MtCreditBucketService
  ): MtCreditBucketService {
    return Mockito.mock(MtCreditBucketService::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun machineTranslationChunkProcessorMock(
    real: MachineTranslationChunkProcessor
  ): MachineTranslationChunkProcessor {
    return Mockito.mock(MachineTranslationChunkProcessor::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun preTranslationByTmChunkProcessorMock(
    real: PreTranslationByTmChunkProcessor
  ): PreTranslationByTmChunkProcessor {
    return Mockito.mock(PreTranslationByTmChunkProcessor::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun azureFileStorageFactoryMock(
    real: AzureFileStorageFactory
  ): AzureFileStorageFactory {
    return Mockito.mock(AzureFileStorageFactory::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun s3FileStorageFactoryMock(
    real: S3FileStorageFactory
  ): S3FileStorageFactory {
    return Mockito.mock(S3FileStorageFactory::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun mtServiceMock(
    real: MtService
  ): MtService {
    return Mockito.mock(MtService::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun entityManagerMock(
    real: EntityManager
  ): EntityManager {
    return Mockito.mock(EntityManager::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun currentDateProviderMock(
    real: CurrentDateProvider
  ): CurrentDateProvider {
    return Mockito.mock(CurrentDateProvider::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun deleteKeysChunkProcessorMock(
    real: DeleteKeysChunkProcessor
  ): DeleteKeysChunkProcessor {
    return Mockito.mock(DeleteKeysChunkProcessor::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun automationChunkProcessorMock(
    real: AutomationChunkProcessor
  ): AutomationChunkProcessor {
    return Mockito.mock(AutomationChunkProcessor::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun batchJobProjectLockingManagerMock(
    real: BatchJobProjectLockingManager
  ): BatchJobProjectLockingManager {
    return Mockito.mock(BatchJobProjectLockingManager::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun progressManagerMock(
    real: ProgressManager
  ): ProgressManager {
    return Mockito.mock(ProgressManager::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun batchJobCancellationManagerMock(
    real: BatchJobCancellationManager
  ): BatchJobCancellationManager {
    return Mockito.mock(BatchJobCancellationManager::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }

  @MockWrappedBean
  @Bean
  @Primary
  fun tokenBucketManagerMock(
    real: TokenBucketManager
  ): TokenBucketManager {
    return Mockito.mock(TokenBucketManager::class.java, AdditionalAnswers.delegatesTo<Any>(real))
  }
}
