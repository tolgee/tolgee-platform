package io.tolgee.component.reporting

import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

@Component
class SdkInfoProvider() {
  fun getSdkInfo(request: HttpServletRequest? = null): Map<String, String?>? {
    val safeRequest = request ?: getRequest() ?: return null
    return mapOf(
      "sdkType" to safeRequest.getHeader("X-Tolgee-SDK-Type"),
      "sdkVersion" to safeRequest.getHeader("X-Tolgee-SDK-Version")
    )
  }

  fun getRequest(): HttpServletRequest? {
    val requestAttributes = RequestContextHolder.getRequestAttributes()
    if (requestAttributes is ServletRequestAttributes) {
      return (requestAttributes).request
    }
    return null
  }
}
