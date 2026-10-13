import { MuiTelInputInfo, matchIsValidTel } from 'mui-tel-input';

/**
 * `info` stays null until the field is touched, so emptiness has to come from
 * the value while a prefilled number is still untouched.
 */
export function readPhoneValue(
  value: string,
  info: MuiTelInputInfo | null
): string | null {
  const isEmpty = info ? !info.nationalNumber : !value;
  if (isEmpty) {
    return '';
  }
  if (!matchIsValidTel(value)) {
    return null;
  }
  return info?.numberValue ?? value;
}
