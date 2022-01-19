package io.tolgee

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.translate.AmazonTranslate
import com.amazonaws.services.translate.AmazonTranslateClient
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MtServicesConfiguration(
  private val googleMachineTranslationProperties: GoogleMachineTranslationProperties,
  private val awsMachineTranslationProperties: AwsMachineTranslationProperties
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
  fun getAwsTranslationService(): AmazonTranslate? {

    if (awsMachineTranslationProperties.accessKey != null) {
      return AmazonTranslateClient.builder().withCredentials(
        AWSStaticCredentialsProvider(
          BasicAWSCredentials(
            awsMachineTranslationProperties.accessKey,
            awsMachineTranslationProperties.secretKey
          )
        )
      ).withRegion(awsMachineTranslationProperties.region).build()
    }
    return null
  }
}
