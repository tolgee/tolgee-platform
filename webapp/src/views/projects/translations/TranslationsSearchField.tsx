import { FunctionComponent, useContext } from 'react';
import { T } from '@tolgee/react';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { TranslationListContext } from './TtranslationsGridContextProvider';

export const TranslationsSearchField: FunctionComponent = (props) => {
  const listContext = useContext(TranslationListContext);
  const initial = listContext.listLoadable
    ? listContext.listLoadable!.data!.params!.search || ''
    : '';

  return (
    <SearchField
      id="standard-search"
      label={<T>translations_search_field_label</T>}
      type="search"
      initial={initial}
      onSearch={(search) =>
        listContext.loadData(search, listContext.perPage, 0)
      }
    />
  );
};
