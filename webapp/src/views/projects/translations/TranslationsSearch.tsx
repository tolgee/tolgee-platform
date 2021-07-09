import SearchField from 'tg.component/common/form/fields/SearchField';
import { useContextSelector } from 'use-context-selector';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';

export const TranslationsSearch = () => {
  const search = useContextSelector(TranslationsContext, (v) => v.search);
  const dispatch = useTranslationsDispatch();

  const handleSearchChange = (value: string) => {
    dispatch({ type: 'SET_SEARCH', payload: value });
  };

  return (
    <SearchField label={''} initial={search} onSearch={handleSearchChange} />
  );
};
