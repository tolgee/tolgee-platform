import { useCurrentLanguage } from './useCurrentLanguage';

export const useMoneyFormatter = () => {
  const language = useCurrentLanguage();
  return (number: number, options?: Intl.NumberFormatOptions) =>
    new Intl.NumberFormat(language, {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
      ...options,
    }).format(number);
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
