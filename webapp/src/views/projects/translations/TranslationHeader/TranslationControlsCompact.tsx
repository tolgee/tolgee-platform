import { useState } from 'react';
import {
  Add,
  AppsRounded,
  Clear,
  FilterList,
  Search,
  ViewListRounded,
} from '@mui/icons-material';
import { Badge, Button, ButtonGroup, IconButton, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import LanguageIcon from '@mui/icons-material/Language';

import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

import TranslationsSearchField from './TranslationsSearchField';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { ViewMode } from '../context/types';
import { useActiveFilters } from '../Filters/useActiveFilters';
import { FiltersMenu } from '../Filters/FiltersMenu';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesSelect/LanguagesMenu';
import { StickyHeader } from './StickyHeader';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';

const StyledContainer = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-left: ${({ theme }) => theme.spacing(-2)};
  margin-right: ${({ theme }) => theme.spacing(-2)};
  padding: ${({ theme }) => theme.spacing(0, 1.5)};
  z-index: ${({ theme }) => theme.zIndex.appBar + 1};
  transition: transform 0.2s ease-in-out;
  padding-bottom: 4px;
  padding-top: 9px;
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
  const { t } = useTranslate();

  const { setSearch, changeView, selectLanguages } = useTranslationsActions();
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const [anchorFiltersEl, setAnchorFiltersEl] =
    useState<HTMLButtonElement | null>(null);
  const [anchorLanguagesEl, setAnchorLanguagesEl] =
    useState<HTMLButtonElement | null>(null);

  const handleSearchChange = (value: string) => {
    setSearch(value);
  };

  const activeFilters = useActiveFilters();

  const handleLanguageChange = (languages: string[]) => {
    selectLanguages(languages);
  };

  const handleViewChange = (val: ViewMode) => {
    changeView(val);
  };

  const handleAddTranslation = () => {
    onDialogOpen();
  };

  return (
    <StickyHeader height={45}>
      <StyledContainer>
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

              {projectPermissions.satisfiesPermission('keys.edit') && (
                <QuickStartHighlight itemKey="add_key">
                  <StyledIconButton
                    color="primary"
                    size="small"
                    onClick={handleAddTranslation}
                    data-cy="translations-add-button"
                  >
                    <Add />
                  </StyledIconButton>
                </QuickStartHighlight>
              )}
            </StyledSpaced>
          </>
        )}
      </StyledContainer>
    </StickyHeader>
  );
};
