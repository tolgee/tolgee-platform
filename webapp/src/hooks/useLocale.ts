import { useCurrentLanguage } from './useCurrentLanguage';

export const useMoneyFormatter = () => {
  const language = useCurrentLanguage();
  return (number: number | undefined, options?: Intl.NumberFormatOptions) => {
    const maximumFractionDigits = options?.maximumFractionDigits ?? 2;
    const rounded = Number(number?.toFixed(maximumFractionDigits)) || 0;

    return new Intl.NumberFormat(language, {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits,
      minimumFractionDigits: 2,
      ...options,
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
