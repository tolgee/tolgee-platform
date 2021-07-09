import { useState } from 'react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  IconButton,
  Button,
} from '@material-ui/core';
import { ViewListRounded, TableChart, Add } from '@material-ui/icons';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useContextSelector } from 'use-context-selector';
import { T } from '@tolgee/react';

import { TranslationNewDialog } from './TranslationNewDialog';
import {
  TranslationsContext,
  useTranslationsDispatch,
  ViewType,
} from './TranslationsContext';

export const TranslationsHeader = () => {
  const search = useContextSelector(TranslationsContext, (v) => v.search);
  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (v) => v.selectedLanguages
  );
  const view = useContextSelector(TranslationsContext, (v) => v.view);

  const [newDialogOpen, setNewDialogOpen] = useState(false);

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

  const handleViewChange = (val: ViewType) => {
    dispatch({ type: 'CHANGE_VIEW', payload: val });
  };

  const handleAfterAdd = () => {
    dispatch({ type: 'UPDATE_LANGUAGES' });
  };

  return (
    <Box display="flex" alignItems="center">
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
      <IconButton
        color={view === 'TABLE' ? 'primary' : undefined}
        onClick={() => handleViewChange('TABLE')}
      >
        <TableChart />
      </IconButton>
      <IconButton
        color={view === 'LIST' ? 'primary' : undefined}
        onClick={() => handleViewChange('LIST')}
      >
        <ViewListRounded />
      </IconButton>

      <Button
        startIcon={<Add />}
        color="primary"
        size="small"
        variant="contained"
        onClick={() => setNewDialogOpen(true)}
      >
        <T>language_create_add</T>
      </Button>

      {newDialogOpen && (
        <TranslationNewDialog
          onClose={() => setNewDialogOpen(false)}
          onAdd={handleAfterAdd}
        />
      )}
    </Box>
  );
};
