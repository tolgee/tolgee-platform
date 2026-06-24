package io.tolgee.facade

import io.tolgee.configuration.tolgee.E2eRuntimeMutable
import io.tolgee.dtos.request.SetPropertyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import org.springframework.stereotype.Component
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation

@Component
class InternalPropertiesSetterFacade {
  fun setProperty(
    root: Any,
    setPropertyDto: SetPropertyDto,
    onSet: (() -> Unit)? = null,
  ) {
    val name = setPropertyDto.name
    var instance: Any = root
    name.split(".").let { namePath ->
      namePath.forEachIndexed { idx, property ->
        val isLast = idx == namePath.size - 1
        val props = instance::class.declaredMemberProperties
        val prop = props.find { it.name == property } ?: throw NotFoundException()
        if (isLast) {
          (prop as? KMutableProperty1<Any, Any?>)?.let {
            if (!it.hasAnnotation<E2eRuntimeMutable>()) {
              io.tolgee.constants.Message.PROPERTY_NOT_MUTABLE
            }
            it.set(instance, setPropertyDto.value)
            onSet?.invoke()
            return
          } ?: throw BadRequestException(io.tolgee.constants.Message.PROPERTY_NOT_MUTABLE)
        }
        instance = (prop as KProperty1<Any, Any?>).get(instance)
          ?: throw NotFoundException()
      }
    }
  }
}
