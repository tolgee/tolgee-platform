import { default as React, useMemo } from 'react';
import { T } from '@tolgee/react';

const expiryPredefinedOptionsDays = [7, 30, 60, 90];

export type ExpirationDateOptions = {
  value: string;
  time: number | null;
  label: JSX.Element;
}[];

export const useExpirationDateOptions = () => {
  return useMemo(() => {
    const now = new Date().getTime();
    const options = expiryPredefinedOptionsDays.map((days) => {
      const time = (now + days * 24 * 60 * 60 * 1000) as number | null;
      return {
        time: time,
        value: `${time}`,
        label: (
          <T params={{ days: days }} keyName="expiration-date-days-option">
            {'{days} days'}
          </T>
        ),
      };
    });
    options.push({
      value: 'never',
      label: <T keyName="expiration-never-option">{'Never expires'}</T>,
      time: null,
    });
    options.push({
      value: 'custom',
      label: <T keyName="expiration-custom-option">{'Custom'}</T>,
      time: null,
    });
    return options;
  }, []);
};
