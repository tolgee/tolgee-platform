import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';

export const useMoneyFormatter = () => {
  const language = useCurrentLanguage();
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
      currency: 'EUR',
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
