import { getCountries } from 'libphonenumber-js';
import type { MuiTelInputCountry } from 'mui-tel-input';

const FALLBACK_COUNTRY: MuiTelInputCountry = 'US';

// There is no GeoIP and nginx passes no country header, so locale preferences
// are the only signal available - they indicate language, not location.
export function detectCountry(): MuiTelInputCountry {
  const supported = new Set<string>(getCountries());
  const tags = [...(navigator.languages ?? []), navigator.language].filter(
    Boolean
  );

  for (const tag of tags) {
    let region: string | undefined;
    try {
      region = new Intl.Locale(tag).region ?? undefined;
    } catch {
      region = tag.split('-')[1];
    }
    // Intl gives UN M49 codes such as "419" for es-419, which are not countries.
    if (region && /^[A-Za-z]{2}$/.test(region)) {
      const isoCode = region.toUpperCase();
      if (supported.has(isoCode)) {
        return isoCode as MuiTelInputCountry;
      }
    }
  }

  return FALLBACK_COUNTRY;
}
