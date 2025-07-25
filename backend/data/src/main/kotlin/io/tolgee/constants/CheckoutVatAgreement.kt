package io.tolgee.constants

object CheckoutVatAgreement {
  private val translations = mapOf(
    SupportedLocale.DEFAULT to "Subscription is purchased by a VAT-taxable entity",
    SupportedLocale.CS to "Předplatné je zakoupeno plátcem DPH",
    SupportedLocale.FR to "L'abonnement est acheté par une entité assujettie à la TVA",
    SupportedLocale.ES to "La suscripción es adquirida por una entidad sujeta al IVA",
    SupportedLocale.DE to "Das Abonnement wird von einem umsatzsteuerpflichtigen Unternehmen gekauft",
    SupportedLocale.PT to "A assinatura é adquirida por uma entidade sujeita ao IVA",
    SupportedLocale.DA to "Abonnementet er købt af en momspligtig enhed",
    SupportedLocale.JA to "サブスクリプションは消費税課税事業者によって購入されます"
  )

  fun translate(locale: SupportedLocale): String {
    return translations[locale] ?: translations[SupportedLocale.DEFAULT]!!
  }
}
