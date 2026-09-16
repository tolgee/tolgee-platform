import { describe, expect, it } from 'vitest';
import { formatMoney, moneyFractionDigits } from './useLocale';

describe('money currency', () => {
  it('formats in the currency it is given', () => {
    expect(formatMoney('en', 'EUR', 12)).toContain('€');
    expect(formatMoney('en', 'USD', 12)).toContain('$');
  });

  it('lets a caller override the surrounding currency', () => {
    // An invoice keeps the currency it was issued in, even inside a USD organization.
    expect(formatMoney('en', 'USD', 12, { currency: 'EUR' })).toContain('€');
  });
});

describe('money fraction digits', () => {
  it('defaults to two digits either side', () => {
    expect(moneyFractionDigits()).toEqual({
      maximumFractionDigits: 2,
      minimumFractionDigits: 2,
    });
  });

  it('pulls the minimum down with the maximum, so Intl does not throw', () => {
    expect(moneyFractionDigits({ maximumFractionDigits: 0 })).toEqual({
      maximumFractionDigits: 0,
      minimumFractionDigits: 0,
    });

    expect(() =>
      new Intl.NumberFormat('en', {
        style: 'currency',
        currency: 'EUR',
        ...moneyFractionDigits({ maximumFractionDigits: 0 }),
      }).format(12.5)
    ).not.toThrow();
  });

  it('leaves an explicit minimum alone when it fits', () => {
    expect(
      moneyFractionDigits({
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      })
    ).toEqual({ maximumFractionDigits: 2, minimumFractionDigits: 0 });
  });
});
