import { useState } from 'react';
import {
  ViewListRounded,
  AppsRounded,
  Add,
  Search,
  FilterList,
  Clear,
} from '@mui/icons-material';
import { Badge, Button, ButtonGroup, IconButton, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import LanguageIcon from '@mui/icons-material/Language';

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

const StyledControls = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -12px -5px;
  margin-left: ${({ theme }) => theme.spacing(-2)};
  margin-right: ${({ theme }) => theme.spacing(-2)};
  padding: ${({ theme }) => theme.spacing(0, 1.5)};
  position: sticky;
  top: 50px;
  height: 50px;
  z-index: ${({ theme }) => theme.zIndex.appBar + 1};
  background: ${({ theme }) => theme.palette.background.default};
  transition: transform 0.2s ease-in-out;
  padding-bottom: 4px;
  padding-top: 7px;
`;

const StyledShadow = styled('div')`
  background: ${({ theme }) => theme.palette.background.default};
  height: 1px;
  position: sticky;
  z-index: ${({ theme }) => theme.zIndex.appBar};
  margin-left: ${({ theme }) => theme.spacing(-1)};
  margin-right: ${({ theme }) => theme.spacing(-1)};
  -webkit-box-shadow: 0px -1px 7px 0px #000000;
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 1px 6px 0px #000000, 0px 1px 6px 0px #000000'
      : '0px -1px 7px 0px #000000'};
  top: 99px;
  transition: all 0.25s;
`;

const StyledSpaced = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(0, 1)};
`;

const StyledSearchSpaced = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  padding-right: ${({ theme }) => theme.spacing(1)};
  flex-grow: 1;
  position: relative;
`;

const StyledSearch = styled(TranslationsSearchField)`
  min-width: 200px;
`;

const StyledToggleButton = styled(Button)`
  padding: 0px 2px;
  height: 35px;
  min-height: 35px;
`;

const StyledIconButton = styled(IconButton)`
  width: 38px;
  height: 38px;
`;

const StyledButtonWrapper = styled('div')`
  margin: -8px 0px;
`;

type Props = {
  onDialogOpen: () => void;
};

export const TranslationControlsCompact: React.FC<Props> = ({
  onDialogOpen,
}) => {
  const projectPermissions = useProjectPermissions();
  const [searchOpen, setSearchOpen] = useState(false);
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const t = useTranslate();

  const dispatch = useTranslationsDispatch();
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

  const handleAddTranslation = () => {
    onDialogOpen();
  };

  const trigger = useTopBarHidden();

  return (
    <>
      <StyledControls
        style={{
          transform: trigger ? 'translate(0px, -50px)' : 'translate(0px, 0px)',
        }}
      >
        {searchOpen ? (
          <StyledSearchSpaced>
            <StyledSearch
              value={search || ''}
              onSearchChange={handleSearchChange}
              label={null}
              variant="outlined"
              placeholder={t('standard_search_label')}
              style={{
                height: 35,
                maxWidth: 'unset',
                width: '100%',
              }}
            />
            <StyledIconButton size="small" onClick={() => setSearchOpen(false)}>
              <Clear />
            </StyledIconButton>
          </StyledSearchSpaced>
        ) : (
          <>
            <StyledSpaced>
              <Badge color="primary" badgeContent={search.length} variant="dot">
                <StyledButtonWrapper>
                  <StyledIconButton
                    size="small"
                    onClick={() => setSearchOpen(true)}
                  >
                    <Search />
                  </StyledIconButton>
                </StyledButtonWrapper>
              </Badge>

              <Badge color="primary" badgeContent={activeFilters?.length}>
                <StyledButtonWrapper>
                  <StyledIconButton
                    size="small"
                    onClick={(e) => setAnchorFiltersEl(e.currentTarget)}
                  >
                    <FilterList />
                  </StyledIconButton>
                </StyledButtonWrapper>
              </Badge>

              <FiltersMenu
                anchorEl={anchorFiltersEl}
                onClose={() => setAnchorFiltersEl(null)}
              />
            </StyledSpaced>

            <StyledSpaced>
              <StyledIconButton
                size="small"
                onClick={(e) => setAnchorLanguagesEl(e.currentTarget)}
              >
                <LanguageIcon />
              </StyledIconButton>

              <LanguagesMenu
                anchorEl={anchorLanguagesEl}
                onClose={() => setAnchorLanguagesEl(null)}
                onChange={handleLanguageChange}
                value={selectedLanguages}
                languages={languages}
              />

              <ButtonGroup>
                <StyledToggleButton
                  color={view === 'LIST' ? 'primary' : 'default'}
                  onClick={() => handleViewChange('LIST')}
                  data-cy="translations-view-list-button"
                >
                  <ViewListRounded />
                </StyledToggleButton>
                <StyledToggleButton
                  color={view === 'TABLE' ? 'primary' : 'default'}
                  onClick={() => handleViewChange('TABLE')}
                  data-cy="translations-view-table-button"
                >
                  <AppsRounded />
                </StyledToggleButton>
              </ButtonGroup>

              {projectPermissions.satisfiesPermission(
                ProjectPermissionType.EDIT
              ) && (
                <StyledIconButton
                  color="primary"
                  size="small"
                  onClick={handleAddTranslation}
                  data-cy="translations-add-button"
                >
                  <Add />
                </StyledIconButton>
              )}
            </StyledSpaced>
          </>
        )}
      </StyledControls>
      <StyledShadow
        style={{
          transform: trigger ? 'translate(0px, -50px)' : 'translate(0px, 0px)',
        }}
      />
    </>
  );
};
