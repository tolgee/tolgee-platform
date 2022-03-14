import { ViewListRounded, AppsRounded, Add, Delete } from '@material-ui/icons';
import { Button, ButtonGroup, IconButton, makeStyles } from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import TranslationsSearchField from './TranslationsSearchField';

import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { Filters } from '../Filters/Filters';
import { ViewMode } from '../context/types';
import { useTopBarHidden } from 'tg.component/layout/TopBar/TopBarContext';

const useStyles = makeStyles((theme) => ({
  controls: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    flexWrap: 'wrap',
    margin: '-12px -5px -10px -5px',
    marginLeft: -theme.spacing(2),
    marginRight: -theme.spacing(2),
    padding: theme.spacing(0, 1.5),
    position: 'sticky',
    top: 50,
    zIndex: theme.zIndex.appBar + 1,
    background: theme.palette.background.default,
    transition: 'all 0.20s ease-in-out',
    paddingBottom: 3,
    paddingTop: 8,
  },
  shadow: {
    background: theme.palette.background.default,
    height: 1,
    position: 'sticky',
    zIndex: theme.zIndex.appBar,
    marginLeft: -theme.spacing(1),
    marginRight: -theme.spacing(1),
    '-webkit-box-shadow': '0px -1px 7px 0px #000000',
    'box-shadow': '0px -1px 7px 0px #000000',
    top: 110,
    transition: 'all 0.25s',
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
  toggleButton: {
    padding: '4px 8px',
  },
  modal: {
    transition: 'margin-bottom 0.2s',
  },
}));

type Props = {
  onDialogOpen: () => void;
};

export const TranslationControls: React.FC<Props> = ({ onDialogOpen }) => {
  const classes = useStyles();
  const projectPermissions = useProjectPermissions();
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const t = useTranslate();

  const dispatch = useTranslationsDispatch();
  const selection = useTranslationsSelector((v) => v.selection);
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);

  const handleSearchChange = (value: string) => {
    dispatch({ type: 'SET_SEARCH', payload: value });
  };

  const handleLanguageChange = (languages: string[]) => {
    dispatch({
      type: 'SELECT_LANGUAGES',
      payload: languages,
    });
  };

  const handleViewChange = (val: ViewMode) => {
    dispatch({ type: 'CHANGE_VIEW', payload: val });
  };

  const handleDelete = () => {
    dispatch({ type: 'DELETE_TRANSLATIONS' });
  };

  const handleAddTranslation = () => {
    onDialogOpen();
  };

  const trigger = useTopBarHidden();

  return (
    <>
      <div
        className={classes.controls}
        style={{
          transform: trigger ? 'translate(0px, -55px)' : 'translate(0px, 0px)',
        }}
      >
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
          <LanguagesSelect
            onChange={handleLanguageChange}
            value={selectedLanguages || []}
            languages={languages || []}
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
      <div
        className={classes.shadow}
        style={{
          transform: trigger ? 'translate(0px, -55px)' : 'translate(0px, 0px)',
        }}
      />
    </>
  );
};
