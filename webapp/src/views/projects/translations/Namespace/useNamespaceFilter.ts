import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import {
  encodeFilter,
  toggleFilter,
} from 'tg.component/translation/translationFilters/tools';

export function useNamespaceFilter(namespace: string | undefined) {
  const filters = useTranslationsSelector((c) => c.filters);
  const { setFilters } = useTranslationsActions();

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
    setFilters(newFilters);
  };
  return { isActive, toggle };
}
