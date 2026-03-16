package io.tolgee.ee.service.qa

object QaPluralCheckHelper {
  /**
   * Runs a check function per plural variant with automatic position mapping.
   *
   * For non-plural text (or when textVariants is null): passes through to checkFn with
   * the full text/baseText and returns results as-is.
   *
   * For plural text: iterates over variants (or only [params.activeVariant] if specified),
   * finds the best matching base variant, calls checkFn with per-variant text,
   * and maps result positions from variant-relative to full-ICU-text-relative.
   *
   * @param params The QA check params containing text, variants, offsets, and activeVariant
   * @param checkFn The per-variant check logic. Receives (variantText, baseVariantText)
   *                and returns issues with positions relative to the variant text.
   */
  fun runPerVariant(
    params: QaCheckParams,
    checkFn: (text: String, baseText: String?) -> List<QaCheckResult>,
  ): List<QaCheckResult> {
    val textVariants = params.textVariants
    val offsets = params.textVariantOffsets

    if (!params.isPlural || textVariants == null) {
      return checkFn(params.text, params.baseText)
    }

    val baseVariants = params.baseTextVariants
    val results = mutableListOf<QaCheckResult>()

    val variantsToCheck =
      if (params.activeVariant != null) {
        textVariants.filterKeys { it == params.activeVariant }
      } else {
        textVariants
      }

    for ((variantKey, variantText) in variantsToCheck) {
      val baseVariantText = baseVariants?.get(variantKey) ?: baseVariants?.get("other")
      val offset = offsets?.get(variantKey) ?: 0

      val variantResults = checkFn(variantText, baseVariantText)
      results.addAll(
        variantResults.map { result ->
          result.copy(
            positionStart = result.positionStart + offset,
            positionEnd = result.positionEnd + offset,
            pluralVariant = variantKey,
          )
        },
      )
    }

    return results
  }
}
