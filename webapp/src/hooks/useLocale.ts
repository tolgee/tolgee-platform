import { createContext, useContext } from 'react';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';

/**
 * The currency the surrounding screen is denominated in. Money is formatted with it unless a
 * caller passes its own — an invoice, for instance, keeps the currency it was issued in even
 * after the organization moves to another one.
 */
export const MoneyCurrencyContext = createContext<string>('EUR');

export const MoneyCurrencyProvider = MoneyCurrencyContext.Provider;

export const moneyFractionDigits = (options?: Intl.NumberFormatOptions) => {
  const maximumFractionDigits = options?.maximumFractionDigits ?? 2;
  return {
    maximumFractionDigits,
    minimumFractionDigits: Math.min(
      options?.minimumFractionDigits ?? 2,
      maximumFractionDigits
    ),
  };
};

export const formatMoney = (
  language: string | undefined,
  currency: string,
  number: number | undefined,
  options?: Intl.NumberFormatOptions
) => {
  const { maximumFractionDigits, minimumFractionDigits } =
    moneyFractionDigits(options);
  const rounded = Number(number?.toFixed(maximumFractionDigits)) || 0;

  return new Intl.NumberFormat(language, {
    style: 'currency',
    currency,
    ...options,
    maximumFractionDigits,
    minimumFractionDigits,
  }).format(rounded);
};

export const useMoneyFormatter = () => {
  const language = useCurrentLanguage();
  const contextCurrency = useContext(MoneyCurrencyContext);
  return (number: number | undefined, options?: Intl.NumberFormatOptions) =>
    formatMoney(language, contextCurrency, number, options);
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
