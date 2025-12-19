import { useRef, useState } from 'react';
import {
  Plus,
  XClose,
  FilterLines,
  SearchSm,
  LayoutGrid02,
  LayoutLeft,
  Globe02,
} from '@untitled-ui/icons-react';
import {
  Badge,
  Button,
  ButtonGroup,
  IconButton,
  styled,
  Tooltip,
  useMediaQuery,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { HeaderSearchField } from 'tg.component/layout/HeaderSearchField';
import { TranslationFiltersPopup } from 'tg.views/projects/translations/TranslationFilters/TranslationFiltersPopup';
import { TranslationSortMenu } from 'tg.component/translation/translationSort/TranslationSortMenu';
import { Sort } from 'tg.component/CustomIcons';
import { useProject } from 'tg.hooks/useProject';
import { countFilters } from 'tg.views/projects/translations/TranslationFilters/summary';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesSelect/LanguagesMenu';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { ViewMode } from '../context/types';

const StyledLanguagesSelect = styled(LanguagesSelect)`
  & .MuiInputBase-root {
    height: 35px;
    width: 200px;
  }
`;

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  margin-left: ${({ theme }) => theme.spacing(-1)};
  margin-right: ${({ theme }) => theme.spacing(-2)};
  padding: ${({ theme }) => theme.spacing(0, 1.5)};
  z-index: ${({ theme }) => theme.zIndex.appBar + 1};
  transition: transform 0.2s ease-in-out;
`;

const StyledSpaced = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(0, 1)};
`;

const StyledSearchSpaced = styled('div')`
  display: grid;
  align-items: center;
  flex-grow: 1;
  grid-template-columns: 1fr auto;
  gap: ${({ theme }) => theme.spacing(0.5)};
  position: relative;
  grid-column: 1 / -1;
`;

const StyledSearch = styled(HeaderSearchField)`
  min-width: 200px;
`;

const StyledToggleButton = styled(Button)`
  padding: 0px 2px;
  height: 35px;
  min-height: 35px;
  & svg {
    width: 22px;
    height: 22px;
  }
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
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const isSuperSmall = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 600}px)`
  );
  const projectPermissions = useProjectPermissions();
  const [searchOpen, setSearchOpen] = useState(false);
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const order = useTranslationsSelector((v) => v.order);
  const { t } = useTranslate();
  const project = useProject();
  const allLanguages = useTranslationsSelector((c) => c.languages);

  const { setSearch, changeView, selectLanguages, setOrder } =
    useTranslationsActions();
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const selectedLanguagesMapped =
    allLanguages?.filter((l) => selectedLanguages?.includes(l.tag)) ?? [];

  const [anchorLanguagesEl, setAnchorLanguagesEl] =
    useState<HTMLButtonElement | null>(null);
  const [anchorSortEl, setAnchorSortEl] = useState<HTMLButtonElement | null>(
    null
  );
  const anchorFilters = useRef<HTMLButtonElement>(null);
  const [filtersOpen, setFiltersOpen] = useState(false);

  const handleSearchChange = (value: string) => {
    setSearch(value);
  };
  const filters = useTranslationsSelector((c) => c.filters);
  const { setFilters, addFilter, removeFilter } = useTranslationsActions();

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
            setSearchOpen={setSearchOpen}
          />
          <StyledIconButton size="small" onClick={() => setSearchOpen(false)}>
            <XClose />
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
                  <SearchSm />
                </StyledIconButton>
              </StyledButtonWrapper>
            </Badge>

            <Badge color="primary" badgeContent={countFilters(filters)}>
              <StyledButtonWrapper>
                <StyledIconButton
                  size="small"
                  onClick={() => setFiltersOpen(true)}
                  ref={anchorFilters}
                >
                  <FilterLines />
                </StyledIconButton>
              </StyledButtonWrapper>
            </Badge>
            {filtersOpen && (
              <TranslationFiltersPopup
                value={filters}
                anchorEl={anchorFilters.current!}
                onClose={() => setFiltersOpen(false)}
                actions={{ setFilters, removeFilter, addFilter }}
                projectId={project.id}
                selectedLanguages={selectedLanguagesMapped}
                showClearButton
              />
            )}
            <Tooltip title={t('translation_controls_sort_tooltip')}>
              <Badge
                color="primary"
                variant="dot"
                badgeContent={order === 'keyName' ? 0 : 1}
                overlap="circular"
              >
                <StyledIconButton
                  size="small"
                  onClick={(e) => setAnchorSortEl(e.currentTarget)}
                  data-cy="translation-controls-sort"
                >
                  <Sort />
                </StyledIconButton>
              </Badge>
            </Tooltip>

            <TranslationSortMenu
              anchorEl={anchorSortEl}
              onClose={() => setAnchorSortEl(null)}
              onChange={setOrder}
              value={order}
            />
          </StyledSpaced>

          <StyledSpaced>
            {isSuperSmall ? (
              <>
                <StyledIconButton
                  size="small"
                  onClick={(e) => setAnchorLanguagesEl(e.currentTarget)}
                >
                  <Globe02 />
                </StyledIconButton>

                <LanguagesMenu
                  anchorEl={anchorLanguagesEl}
                  onClose={() => setAnchorLanguagesEl(null)}
                  onChange={handleLanguageChange}
                  value={selectedLanguages}
                  languages={languages}
                />
              </>
            ) : (
              <StyledLanguagesSelect
                onChange={selectLanguages}
                value={selectedLanguages || []}
                languages={languages || []}
                context="translations"
              />
            )}

            <ButtonGroup>
              <StyledToggleButton
                color={view === 'LIST' ? 'primary' : 'default'}
                onClick={() => handleViewChange('LIST')}
                data-cy="translations-view-list-button"
              >
                <LayoutLeft />
              </StyledToggleButton>
              <StyledToggleButton
                color={view === 'TABLE' ? 'primary' : 'default'}
                onClick={() => handleViewChange('TABLE')}
                data-cy="translations-view-table-button"
              >
                <LayoutGrid02 />
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
                  <Plus />
                </StyledIconButton>
              </QuickStartHighlight>
            )}
          </StyledSpaced>
        </>
      )}
    </StyledContainer>
  );
};
