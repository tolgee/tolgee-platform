package io.tolgee.activity

import io.tolgee.activity.holders.ActivityHolder
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ActivityFilter(
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  private val activityHolder: ActivityHolder,
  private val applicationContext: ApplicationContext
) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {

    val activityAnnotation = getActivityAnnotation(request)

    if (activityAnnotation != null) {
      activityHolder.activity = applicationContext.getBean(activityAnnotation.activity.java)
    }

    filterChain.doFilter(request, response)
  }

  private fun getActivityAnnotation(request: HttpServletRequest): RequestActivity? {
    val handlerMethod = (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
    return handlerMethod?.getMethodAnnotation(RequestActivity::class.java)
  }
}
