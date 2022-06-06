import { useCurrentLanguage } from '@tolgee/react';

export const useMoneyFormatter = () => {
  const getLang = useCurrentLanguage();
  const formatter = new Intl.NumberFormat(getLang(), {
    style: 'currency',
    currency: 'EUR',
  }).format;
  return formatter;
};

export const useDateFormatter = () => {
  const getLang = useCurrentLanguage();
  const formatter = new Intl.DateTimeFormat(getLang()).format;
  return formatter;
};

export const useNumberFormatter = () => {
  const getLang = useCurrentLanguage();
  const formatter = new Intl.NumberFormat(getLang()).format;
  return formatter;
};
