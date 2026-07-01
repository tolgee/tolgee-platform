/**
 * Contract for the dynamic-decorators endpoint (`manifest.decoratorsUrl`).
 * The Tolgee webapp POSTs a [DecoratorsRequest] describing the rows currently
 * visible; the app replies with a [DecoratorsResponse] of icon decorations to
 * render alongside native row icons.
 */
export type DecoratorsRequest = {
  installId: number
  projectId: number
  /** Key rows in view. */
  keyIds?: number[]
  /** Language columns in view, by tag. */
  languageTags?: string[]
}

/** One decoration the app wants rendered on a key (or key+language) cell. */
export type DecoratorItem = {
  keyId: number
  /** Omit for a key-level decoration; set for a translation-cell decoration. */
  languageTag?: string
  /** Named icon from the platform icon set. */
  icon: string
  tooltip?: string
}

export type DecoratorsResponse = {
  items: DecoratorItem[]
}
