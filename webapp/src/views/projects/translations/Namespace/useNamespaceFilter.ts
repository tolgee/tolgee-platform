import {
  useTranslationsDispatch,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { encodeFilter, toggleFilter } from '../Filters/tools';

export function useNamespaceFilter(namespace: string | undefined) {
  const filters = useTranslationsSelector((c) => c.filters);
  const translationsDispatch = useTranslationsDispatch();

  if (namespace === undefined) {
    return {
      isActive: false,
      toggle: () => {},
    };
  }

  const isActive = filters['filterNamespace']?.includes(namespace);
  const rawFilter = encodeFilter({
    filter: 'filterNamespace',
    value: namespace,
  });
  const toggle = () => {
    const newFilters = toggleFilter(filters, [], rawFilter);
    translationsDispatch({
      type: 'SET_FILTERS',
      payload: newFilters,
    });
  };
  return { isActive, toggle };
}
