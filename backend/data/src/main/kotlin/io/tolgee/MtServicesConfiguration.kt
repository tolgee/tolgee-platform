package io.tolgee

import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.translate.TranslateClient

@Configuration
class MtServicesConfiguration(
  private val googleMachineTranslationProperties: GoogleMachineTranslationProperties,
  private val awsMachineTranslationProperties: AwsMachineTranslationProperties,
) {
  @Bean
  fun getGoogleTranslationService(): Translate? {
    if (googleMachineTranslationProperties.apiKey != null) {
      // setApiKey is deprecated for some reason, but we don't care,
      // since there is no reason in the docs
      @Suppress("DEPRECATION")
      return TranslateOptions
        .newBuilder()
        .setApiKey(googleMachineTranslationProperties.apiKey)
        .build()
        .service
    }
    return null
  }

  @Bean
  fun getAwsTranslationService(): TranslateClient? {
    val chain =
      when (
        awsMachineTranslationProperties.accessKey.isNullOrEmpty() ||
          awsMachineTranslationProperties.secretKey.isNullOrEmpty()
      ) {
        true -> DefaultCredentialsProvider.create()
        false ->
          StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
              awsMachineTranslationProperties.accessKey,
              awsMachineTranslationProperties.secretKey,
            ),
          )
      }

    return TranslateClient
      .builder()
      .credentialsProvider(chain)
      .region(Region.of(awsMachineTranslationProperties.region))
      .build()
  }
}
