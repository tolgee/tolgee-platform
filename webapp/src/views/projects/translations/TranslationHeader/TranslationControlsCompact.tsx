import { useState } from 'react';
import {
  ViewListRounded,
  AppsRounded,
  Add,
  Delete,
  Search,
  FilterList,
  Clear,
} from '@material-ui/icons';
import {
  Badge,
  Button,
  ButtonGroup,
  IconButton,
  makeStyles,
} from '@material-ui/core';
import { useTranslate } from '@tolgee/react';
import LanguageIcon from '@material-ui/icons/Language';

import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';

import TranslationsSearchField from './TranslationsSearchField';
import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { ViewMode } from '../context/types';
import { useTopBarHidden } from 'tg.component/layout/TopBar/TopBarContext';
import { useActiveFilters } from '../Filters/useActiveFilters';
import { FiltersMenu } from '../Filters/FiltersMenu';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesSelect/LanguagesMenu';

const useStyles = makeStyles((theme) => ({
  controls: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    margin: '-12px -5px',
    marginLeft: -theme.spacing(2),
    marginRight: -theme.spacing(2),
    padding: theme.spacing(0, 1.5),
    position: 'sticky',
    top: 50,
    height: 50,
    zIndex: theme.zIndex.appBar + 1,
    background: theme.palette.background.default,
    transition: 'all 0.20s ease-in-out',
    paddingBottom: 4,
    paddingTop: 7,
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
    top: 99,
    transition: 'all 0.25s',
  },
  spaced: {
    display: 'flex',
    alignItems: 'center',
    gap: theme.spacing(1),
    padding: theme.spacing(0, 1),
  },
  searchSpaced: {
    display: 'flex',
    alignItems: 'center',
    gap: theme.spacing(0.5),
    paddingRight: theme.spacing(1),
    flexGrow: 1,
    position: 'relative',
  },
  deleteButton: {
    display: 'flex',
    flexShrink: 1,
    width: 38,
    height: 38,
  },
  search: {
    minWidth: 200,
  },
  toggleButton: {
    padding: '0px 2px',
    height: 35,
    minHeight: 35,
  },
  iconButton: {
    width: 38,
    height: 38,
  },
  buttonWrapper: {
    margin: '-8px 0px',
  },
  modal: {
    transition: 'margin-bottom 0.2s',
  },
}));

type Props = {
  onDialogOpen: () => void;
};

export const TranslationControlsCompact: React.FC<Props> = ({
  onDialogOpen,
}) => {
  const classes = useStyles();
  const projectPermissions = useProjectPermissions();
  const [searchOpen, setSearchOpen] = useState(false);
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const t = useTranslate();

  const dispatch = useTranslationsDispatch();
  const selection = useTranslationsSelector((v) => v.selection);
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const [anchorFiltersEl, setAnchorFiltersEl] =
    useState<HTMLButtonElement | null>(null);
  const [anchorLanguagesEl, setAnchorLanguagesEl] =
    useState<HTMLButtonElement | null>(null);

  const handleSearchChange = (value: string) => {
    dispatch({ type: 'SET_SEARCH', payload: value });
  };

  const activeFilters = useActiveFilters();

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
          transform: trigger ? 'translate(0px, -50px)' : 'translate(0px, 0px)',
        }}
      >
        {searchOpen ? (
          <div className={classes.searchSpaced}>
            <TranslationsSearchField
              value={search || ''}
              onSearchChange={handleSearchChange}
              className={classes.search}
              label={null}
              variant="outlined"
              placeholder={t('standard_search_label')}
              style={{
                height: 35,
                maxWidth: 'unset',
                width: '100%',
              }}
            />
            <IconButton
              size="small"
              className={classes.iconButton}
              onClick={() => setSearchOpen(false)}
            >
              <Clear />
            </IconButton>
          </div>
        ) : (
          <>
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
              <Badge color="primary" badgeContent={search.length} variant="dot">
                <div className={classes.buttonWrapper}>
                  <IconButton
                    size="small"
                    className={classes.iconButton}
                    onClick={() => setSearchOpen(true)}
                  >
                    <Search />
                  </IconButton>
                </div>
              </Badge>

              <Badge color="primary" badgeContent={activeFilters?.length}>
                <div className={classes.buttonWrapper}>
                  <IconButton
                    size="small"
                    className={classes.iconButton}
                    onClick={(e) => setAnchorFiltersEl(e.currentTarget)}
                  >
                    <FilterList />
                  </IconButton>
                </div>
              </Badge>

              <FiltersMenu
                anchorEl={anchorFiltersEl}
                onClose={() => setAnchorFiltersEl(null)}
              />
            </div>

            <div className={classes.spaced}>
              <IconButton
                size="small"
                className={classes.iconButton}
                onClick={(e) => setAnchorLanguagesEl(e.currentTarget)}
              >
                <LanguageIcon />
              </IconButton>

              <LanguagesMenu
                anchorEl={anchorLanguagesEl}
                onClose={() => setAnchorLanguagesEl(null)}
                onChange={handleLanguageChange}
                value={selectedLanguages}
                languages={languages}
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
                <IconButton
                  color="primary"
                  size="small"
                  onClick={handleAddTranslation}
                  className={classes.iconButton}
                  data-cy="translations-add-button"
                >
                  <Add />
                </IconButton>
              )}
            </div>
          </>
        )}
      </div>
      <div
        className={classes.shadow}
        style={{
          transform: trigger ? 'translate(0px, -50px)' : 'translate(0px, 0px)',
        }}
      />
    </>
  );
};
