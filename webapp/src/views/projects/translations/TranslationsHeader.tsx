import { useState } from 'react';
import {
  Button,
  ButtonGroup,
  IconButton,
  Box,
  makeStyles,
} from '@material-ui/core';
import { ViewListRounded, AppsRounded, Add, Delete } from '@material-ui/icons';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useContextSelector } from 'use-context-selector';
import { T } from '@tolgee/react';

import { TranslationNewDialog } from './TranslationNewDialog';
import {
  TranslationsContext,
  useTranslationsDispatch,
  ViewType,
} from './context/TranslationsContext';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { Filters } from './Filters/Filters';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    '& > * + *': {
      marginLeft: 20,
    },
  },
  spaced: {
    display: 'flex',
    '& > * + *': {
      marginLeft: 10,
    },
  },
  [`@media (max-width: ${theme.breakpoints.values.md}px)`]: {
    container: {
      flexDirection: 'column',
      alignItems: 'stretch',
      '& > * + *': {
        marginTop: 10,
        marginLeft: 0,
      },
    },
  },
  search: {
    marginTop: 0,
    marginBottom: 0,
    minWidth: 80,
  },
}));

export const TranslationsHeader = () => {
  const classes = useStyles();
  const projectPermissions = useProjectPermissions();
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
          <Box
            display="flex"
            justifyContent="center"
            alignItems="center"
            marginLeft="-4px"
          >
            <IconButton
              size="small"
              onClick={handleDelete}
              data-cy="translations-delete-button"
            >
              <Delete />
            </IconButton>
          </Box>
        )}
        <SearchField
          initial={search}
          onSearch={handleSearchChange}
          margin="dense"
          variant="outlined"
          className={classes.search}
        />
        <Filters />
      </div>

      <div className={classes.spaced}>
        <LanguagesMenu
          onChange={handleLanguageChange}
          value={selectedLanguages || []}
          languages={
            languages?.map((l) => ({ label: l.name, value: l.tag })) || []
          }
          context="translations"
        />

        <ButtonGroup size="small">
          <Button
            color={view === 'LIST' ? 'primary' : undefined}
            onClick={() => handleViewChange('LIST')}
            data-cy="translations-view-list-button"
            size="small"
          >
            <ViewListRounded />
          </Button>
          <Button
            color={view === 'TABLE' ? 'primary' : undefined}
            onClick={() => handleViewChange('TABLE')}
            data-cy="translations-view-table-button"
            size="small"
          >
            <AppsRounded />
          </Button>
        </ButtonGroup>

        {projectPermissions.satisfiesPermission(ProjectPermissionType.EDIT) && (
          <Button
            startIcon={<Add />}
            color="primary"
            size="small"
            variant="contained"
            aria-label="add"
            onClick={() => setNewDialogOpen(true)}
            data-cy="translations-add-button"
          >
            <T>language_create_add</T>
          </Button>
        )}

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
