package io.tolgee.component.machineTranslation.providers

import com.fasterxml.jackson.annotation.JsonProperty
import io.tolgee.configuration.tolgee.machineTranslation.BaiduMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.security.MessageDigest

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class BaiduApiService(
  private val baiduMachineTranslationProperties: BaiduMachineTranslationProperties,
  private val restTemplate: RestTemplate,
) {
  fun translate(
    text: String,
    sourceTag: String,
    targetTag: String,
  ): String? {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

    val salt = getSalt()

    val appId = baiduMachineTranslationProperties.appId
    val appSecret = baiduMachineTranslationProperties.appSecret
    val action = if (baiduMachineTranslationProperties.action) "1" else "0"
    val signature = sign("$appId$text$salt$appSecret")

    val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
    requestBody.add("appid", appId)
    requestBody.add("q", text)
    requestBody.add("from", sourceTag)
    requestBody.add("to", targetTag)
    requestBody.add("salt", salt)
    requestBody.add("action", action)
    requestBody.add("sign", signature.lowercase())

    val response =
      restTemplate.postForEntity<BaiduResponse>(
        "https://fanyi-api.baidu.com/api/trans/vip/translate",
        requestBody,
      )

    return response.body
      ?.transResult
      ?.first()
      ?.dst
      ?: throw RuntimeException(response.toString())
  }

  private fun getSalt(): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..16)
      .map { charPool.random() }
      .joinToString("")
  }

  private fun sign(text: String): String {
    val instance: MessageDigest = MessageDigest.getInstance("MD5")
    val digest: ByteArray = instance.digest(text.toByteArray())
    val buffer = StringBuffer()
    for (b in digest) {
      val i: Int = b.toInt() and 0xff
      var hexString = Integer.toHexString(i)
      if (hexString.length < 2) {
        hexString = "0$hexString"
      }
      buffer.append(hexString)
    }
    return buffer.toString()
  }

  /**
   * Data structure for mapping the Baidu JSON response objects.
   */
  companion object {
    class BaiduResponse {
      @JsonProperty("trans_result")
      var transResult: List<BaiduTranslation>? = null
    }

    class BaiduTranslation {
      var src: String? = null
      var dst: String? = null
    }
  }
}
