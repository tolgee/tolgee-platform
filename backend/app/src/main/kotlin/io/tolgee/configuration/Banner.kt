package io.tolgee.configuration

import io.tolgee.util.VersionProviderImpl
import org.springframework.core.env.Environment
import java.io.PrintStream
import org.springframework.boot.Banner as SpringBanner

class Banner : SpringBanner {
  override fun printBanner(
    arg0: Environment,
    arg1: Class<*>?,
    arg2: PrintStream,
  ) {
    val blue = "\u001B[34m"
    val red = "\u001B[31m"
    val off = "\u001B[0m"
    arg2.println("$blue                                       ")
    arg2.println("$blue ______     __               _         ")
    arg2.println("$blue/_  __/__  / /__ ____ ___   (_)__      ")
    arg2.println("$blue / / / _ \\/ / _ `/ -_) -_) / / _ \\     ")
    arg2.println("$blue/_/  \\___/_/\\_, /\\__/\\__(_)_/\\___/     ")
    arg2.println("$blue           /___/                       ")
    arg2.println("${red}Version: ${VersionProviderImpl.version}$off")
    arg2.println()
  }
}
