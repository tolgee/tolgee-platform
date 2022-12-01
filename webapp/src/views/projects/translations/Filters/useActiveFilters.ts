import { useTranslationsSelector } from '../context/TranslationsContext';
import { NON_EXCLUSIVE_FILTERS } from './tools';

export const useActiveFilters = () => {
  const filtersObj = useTranslationsSelector((v) => v.filters);

  const activeFilters: string[] = [];
  Object.entries(filtersObj).forEach(([key, value]) => {
    if (!NON_EXCLUSIVE_FILTERS.includes(key)) {
      if (value) {
        activeFilters.push(
          JSON.stringify({
            filter: key,
            value: value,
          })
        );
      }
    } else {
      (value as unknown as string[]).forEach((filterVal) => {
        activeFilters.push(
          JSON.stringify({
            filter: key,
            value: filterVal,
          })
        );
      });
    }
  });

  return activeFilters;
};
