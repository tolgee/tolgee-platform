import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';

export function useNamespaceFilter(namespace: string | undefined) {
  const filters = useTranslationsSelector((c) => c.filters);
  const { addFilter, removeFilter } = useTranslationsActions();

  if (namespace === undefined) {
    return {
      isActive: false,
      toggle: () => {},
    };
  }

  const isActive = filters['filterNamespace']?.includes(namespace);
  const toggle = () => {
    if (isActive) {
      removeFilter('filterNamespace', namespace);
    } else {
      addFilter('filterNamespace', namespace);
    }
  };
  return { isActive, toggle };
}
