import { useState } from 'react';
import { Button, ButtonGroup, makeStyles } from '@material-ui/core';
import { ViewListRounded, AppsRounded, Add, Delete } from '@material-ui/icons';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useContextSelector } from 'use-context-selector';
import { T } from '@tolgee/react';

import { TranslationNewDialog } from './TranslationNewDialog';
import {
  TranslationsContext,
  useTranslationsDispatch,
  ViewType,
} from './TranslationsContext';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';

const useStyles = makeStyles({
  container: {
    display: 'flex',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    marginRight: -40,
  },
  spaced: {
    display: 'flex',
    '& > * + *': {
      marginLeft: 10,
    },
  },
});

export const TranslationsHeader = () => {
  const classes = useStyles();
  const search = useContextSelector(TranslationsContext, (v) => v.search);
  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (v) => v.selectedLanguages
  );
  const view = useContextSelector(TranslationsContext, (v) => v.view);
  const selection = useContextSelector(TranslationsContext, (v) => v.selection);

  const [newDialogOpen, setNewDialogOpen] = useState(false);

  const dispatch = useTranslationsDispatch();

  const handleSearchChange = (value: string) => {
    dispatch({ type: 'SET_SEARCH', payload: value });
  };

  const handleLanguageChange = (languages: string[]) => {
    dispatch({
      type: 'SELECT_LANGUAGES',
      payload: languages,
    });
  };

  const handleViewChange = (val: ViewType) => {
    dispatch({ type: 'CHANGE_VIEW', payload: val });
  };

  const handleAfterAdd = () => {
    dispatch({ type: 'UPDATE_LANGUAGES' });
  };

  const handleDelete = () => {
    dispatch({ type: 'DELETE_TRANSLATIONS', payload: selection });
  };

  return (
    <div className={classes.container}>
      <div className={classes.spaced}>
        {selection.length > 0 && (
          <Button size="small" onClick={handleDelete}>
            <Delete />
          </Button>
        )}
        <SearchField
          label={''}
          initial={search}
          onSearch={handleSearchChange}
        />
      </div>

      <div className={classes.spaced}>
        <LanguagesMenu
          onChange={handleLanguageChange}
          value={selectedLanguages || []}
          languages={
            languages?.map((l) => ({ label: l.name, value: l.tag })) || []
          }
          context="global"
        />

        <ButtonGroup size="small">
          <Button
            color={view === 'LIST' ? 'primary' : undefined}
            onClick={() => handleViewChange('LIST')}
          >
            <ViewListRounded />
          </Button>
          <Button
            color={view === 'TABLE' ? 'primary' : undefined}
            onClick={() => handleViewChange('TABLE')}
          >
            <AppsRounded />
          </Button>
        </ButtonGroup>

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
      </div>
    </div>
  );
};
