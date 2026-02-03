package io.tolgee.component

interface AdditionalCachesProvider {
  fun getAdditionalCaches(): List<String>
}
