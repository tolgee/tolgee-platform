import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

const REGIONAL_INDICATOR_A = 0x1f1e6;
const LETTER_A = 'A'.charCodeAt(0);

// mui-tel-input otherwise renders every flag as an <img> from flagcdn.com, a
// third-party request on the sign-up page.
export function renderCountryFlag(isoCode: string) {
  return (
    <FlagImage
      flagEmoji={countryCodeToFlagEmoji(isoCode)}
      width={26}
      alt={isoCode}
    />
  );
}

function countryCodeToFlagEmoji(isoCode: string): string {
  const letters = isoCode.toUpperCase();
  if (!/^[A-Z]{2}$/.test(letters)) {
    return '🏳️';
  }
  return String.fromCodePoint(
    ...[...letters].map(
      (c) => REGIONAL_INDICATOR_A + c.charCodeAt(0) - LETTER_A
    )
  );
}
