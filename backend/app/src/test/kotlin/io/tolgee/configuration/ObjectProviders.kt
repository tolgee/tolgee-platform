package io.tolgee.configuration

import org.springframework.beans.factory.ObjectProvider

fun <T : Any> providerOf(value: T?): ObjectProvider<T> =
  object : ObjectProvider<T> {
    override fun getObject(vararg args: Any?): T = value!!

    override fun getObject(): T = value!!

    override fun getIfAvailable(): T? = value

    override fun getIfUnique(): T? = value
  }
