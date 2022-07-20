import { useCurrentLanguage } from '@tolgee/react';

export const useMoneyFormatter = () => {
  const getLang = useCurrentLanguage();
  return (number: number, options?: Intl.NumberFormatOptions) =>
    new Intl.NumberFormat(getLang(), {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
      ...options,
    }).format(number);
};

export const useDateFormatter = () => {
  const getLang = useCurrentLanguage();
  return (
    date: number | Date | undefined,
    options?: Intl.DateTimeFormatOptions
  ) => new Intl.DateTimeFormat(getLang(), options).format(date);
};

export const useNumberFormatter = () => {
  const getLang = useCurrentLanguage();
  return (number: number, options?: Intl.NumberFormatOptions) =>
    new Intl.NumberFormat(getLang(), options).format(number);
};
