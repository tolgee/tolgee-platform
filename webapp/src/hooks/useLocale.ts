import { createContext, useContext } from 'react';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';

/**
 * The currency the surrounding screen is denominated in. Money is formatted with it unless a
 * caller passes its own — an invoice, for instance, keeps the currency it was issued in even
 * after the organization moves to another one.
 */
export const MoneyCurrencyContext = createContext<string>('EUR');

export const MoneyCurrencyProvider = MoneyCurrencyContext.Provider;

export const useMoneyFormatter = () => {
  const language = useCurrentLanguage();
  const contextCurrency = useContext(MoneyCurrencyContext);
  return (number: number | undefined, options?: Intl.NumberFormatOptions) => {
    const maximumFractionDigits = options?.maximumFractionDigits ?? 2;
    const rounded = Number(number?.toFixed(maximumFractionDigits)) || 0;
    // Intl throws a RangeError when the minimum exceeds the maximum, so a caller asking for
    // fewer digits than the default minimum must not have to pass both bounds.
    const minimumFractionDigits = Math.min(
      options?.minimumFractionDigits ?? 2,
      maximumFractionDigits
    );

    return new Intl.NumberFormat(language, {
      style: 'currency',
      currency: contextCurrency,
      ...options,
      maximumFractionDigits,
      minimumFractionDigits,
    }).format(rounded);
  };
};

export const useDateFormatter = () => {
  const language = useCurrentLanguage();
  return (
    date: number | Date | undefined,
    options?: Intl.DateTimeFormatOptions
  ) => new Intl.DateTimeFormat(language, options).format(date);
};

export const useNumberFormatter = () => {
  const language = useCurrentLanguage();
  return (number: number, options?: Intl.NumberFormatOptions) =>
    new Intl.NumberFormat(language, options).format(number);
};
