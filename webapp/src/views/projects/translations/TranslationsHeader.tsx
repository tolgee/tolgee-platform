import { Box, Checkbox, FormControlLabel } from '@material-ui/core';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useContextSelector } from 'use-context-selector';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';

export const TranslationsHeader = () => {
  const search = useContextSelector(TranslationsContext, (v) => v.search);
  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (v) => v.selectedLanguages
  );

  const dispatch = useTranslationsDispatch();

  const handleSearchChange = (value: string) => {
    dispatch({ type: 'SET_SEARCH', payload: value });
  };

  const handleLanguageChange = (e) => {
    const value = e.currentTarget.value;
    const newLanguages = selectedLanguages?.includes(value)
      ? selectedLanguages?.filter((v) => v !== value)
      : [...(selectedLanguages || []), value];
    dispatch({
      type: 'SELECT_LANGUAGES',
      payload: newLanguages,
    });
  };

  return (
    <Box display="flex">
      <SearchField label={''} initial={search} onSearch={handleSearchChange} />
      <Box marginLeft="10px">
        {languages?.map((l) => (
          <FormControlLabel
            key={l.id}
            control={
              <Checkbox
                value={l.tag}
                checked={Boolean(selectedLanguages?.includes(l.tag))}
                onChange={handleLanguageChange}
              />
            }
            label={l.name}
          />
        ))}
      </Box>
    </Box>
  );
};
