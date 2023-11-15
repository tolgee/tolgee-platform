package io.tolgee.component.cdn.cachePurging

interface CdnCachePurging {
  fun purgeForPaths(paths: Set<String>)
}
