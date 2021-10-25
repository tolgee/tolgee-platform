import {
  Button,
  ButtonGroup,
  IconButton,
  makeStyles,
  Typography,
  Dialog,
} from '@material-ui/core';
import { ViewListRounded, AppsRounded, Add, Delete } from '@material-ui/icons';
import { useContextSelector } from 'use-context-selector';
import { T, useTranslate } from '@tolgee/react';

import {
  TranslationsContext,
  useTranslationsDispatch,
  ViewType,
} from './context/TranslationsContext';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { Filters } from './Filters/Filters';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { KeyCreateDialog } from './KeyCreateDialog';
import TranslationsSearchField from './TranslationsSearchField';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    alignItems: 'stretch',
    flexDirection: 'column',
  },
  controls: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    margin: -5,
    flexWrap: 'wrap',
  },
  spaced: {
    display: 'flex',
    '& > *': {
      margin: 5,
    },
    flexWrap: 'wrap',
  },
  deleteButton: {
    display: 'flex',
    flexShrink: 1,
    width: 38,
    height: 38,
    marginLeft: 3,
  },
  search: {
    minWidth: 200,
  },
  resultCount: {
    marginLeft: 1,
    marginTop: theme.spacing(),
  },
  toggleButton: {
    padding: '4px 8px',
  },
}));

export const TranslationsHeader = () => {
  const t = useTranslate();
  const classes = useStyles();
  const projectPermissions = useProjectPermissions();
  const search = useContextSelector(TranslationsContext, (v) => v.search);
  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const [newDialog, setNewDialog] = useUrlSearchState('create', {
    defaultVal: 'false',
  });

  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (c) => c.selectedLanguages
  );

  const translationsTotal = useContextSelector(
    TranslationsContext,
    (c) => c.translationsTotal
  );

  const dataReady = useContextSelector(TranslationsContext, (c) => c.dataReady);

  const view = useContextSelector(TranslationsContext, (v) => v.view);
  const selection = useContextSelector(TranslationsContext, (v) => v.selection);

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

  const handleDelete = () => {
    dispatch({ type: 'DELETE_TRANSLATIONS', payload: selection });
  };

  const handleAddTranslation = () => {
    setNewDialog('true');
  };

  return (
    <div className={classes.container}>
      <div className={classes.controls}>
        <div className={classes.spaced}>
          {selection.length > 0 && (
            <IconButton
              className={classes.deleteButton}
              onClick={handleDelete}
              data-cy="translations-delete-button"
            >
              <Delete />
            </IconButton>
          )}
          <TranslationsSearchField
            value={search || ''}
            onSearchChange={handleSearchChange}
            className={classes.search}
            label={null}
            variant="outlined"
            placeholder={t('standard_search_label')}
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

          <ButtonGroup>
            <Button
              color={view === 'LIST' ? 'primary' : undefined}
              onClick={() => handleViewChange('LIST')}
              data-cy="translations-view-list-button"
              className={classes.toggleButton}
            >
              <ViewListRounded />
            </Button>
            <Button
              color={view === 'TABLE' ? 'primary' : undefined}
              onClick={() => handleViewChange('TABLE')}
              data-cy="translations-view-table-button"
              className={classes.toggleButton}
            >
              <AppsRounded />
            </Button>
          </ButtonGroup>

          {projectPermissions.satisfiesPermission(
            ProjectPermissionType.EDIT
          ) && (
            <Button
              startIcon={<Add />}
              color="primary"
              variant="contained"
              onClick={handleAddTranslation}
              data-cy="translations-add-button"
            >
              <T>language_create_add</T>
            </Button>
          )}
        </div>
      </div>
      {translationsTotal ? (
        <div className={classes.resultCount}>
          <Typography
            color="textSecondary"
            variant="body2"
            data-cy="translations-key-count"
          >
            <T parameters={{ count: String(translationsTotal) }}>
              translations_results_count
            </T>
          </Typography>
        </div>
      ) : null}
      {dataReady && newDialog === 'true' && (
        <Dialog
          open={true}
          onClose={() => setNewDialog('false')}
          fullWidth
          maxWidth="md"
          keepMounted={false}
        >
          <KeyCreateDialog onClose={() => setNewDialog('false')} />
        </Dialog>
      )}
    </div>
  );
};
