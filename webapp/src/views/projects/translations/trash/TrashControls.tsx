import React from 'react';
import {
  Badge,
  Checkbox,
  IconButton,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Sort } from 'tg.component/CustomIcons';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { HeaderSearchField } from 'tg.component/layout/HeaderSearchField';
import { TranslationSortMenu } from 'tg.component/translation/translationSort/TranslationSortMenu';
import { TranslationFilters } from '../TranslationFilters/TranslationFilters';
import { FiltersType } from '../TranslationFilters/tools';
import { components } from 'tg.service/apiSchema.generated';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledControls = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: start;
  margin-bottom: 8px;
`;

const StyledSpaced = styled('div')`
  display: flex;
  gap: 10px;
  padding: 0px 5px;
`;

const StyledSearchField = styled(HeaderSearchField)`
  width: 200px;
`;

const StyledResultCount = styled('div')`
  display: flex;
  align-items: center;
  padding: 0px 0px 4px 0px;
  margin-left: 3px;
`;

const StyledCount = styled(Typography)`
  margin-left: 8px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  search: string;
  onSearchChange: (val: string) => void;
  filters: FiltersType;
  filterActions: {
    addFilter: (key: string, value: any) => void;
    removeFilter: (key: string) => void;
    setFilters: (f: any) => void;
  };
  languages: LanguageModel[];
  selectedLanguages: string[];
  onLanguagesChange: (langs: string[]) => void;
  order: string;
  onOrderChange: (value: string) => void;
  defaultOrder: string;
  sortOptions: { value: string; label: string }[];
  totalElements: number;
  allPageSelected: boolean;
  somePageSelected: boolean;
  onSelectAll: () => void;
  projectId: number;
};

export const TrashControls: React.FC<Props> = ({
  search,
  onSearchChange,
  filters,
  filterActions,
  languages,
  selectedLanguages,
  onLanguagesChange,
  order,
  onOrderChange,
  defaultOrder,
  sortOptions,
  totalElements,
  allPageSelected,
  somePageSelected,
  onSelectAll,
  projectId,
}) => {
  const { t } = useTranslate();
  const [anchorSortEl, setAnchorSortEl] = React.useState<HTMLElement | null>(
    null
  );

  const languageCols = languages.filter((l) => selectedLanguages.includes(l.tag));

  return (
    <>
      <StyledControls>
        <StyledSpaced>
          <StyledSearchField
            value={search}
            onSearchChange={onSearchChange}
            label={null}
            variant="outlined"
            placeholder={t('standard_search_label')}
          />
          <TranslationFilters
            value={filters}
            actions={filterActions}
            selectedLanguages={languageCols}
            projectId={projectId}
            filterOptions={{ showDeletedBy: true }}
          />
          <Tooltip title={t('translation_controls_sort_tooltip')}>
            <Badge
              color="primary"
              variant="dot"
              badgeContent={order === defaultOrder ? 0 : 1}
              overlap="circular"
            >
              <IconButton
                onClick={(e) => setAnchorSortEl(e.currentTarget)}
                data-cy="trash-sort-button"
              >
                <Sort />
              </IconButton>
            </Badge>
          </Tooltip>
          <TranslationSortMenu
            value={order}
            onChange={onOrderChange}
            anchorEl={anchorSortEl}
            onClose={() => setAnchorSortEl(null)}
            options={sortOptions}
          />
        </StyledSpaced>

        <StyledSpaced>
          <LanguagesSelect
            onChange={onLanguagesChange}
            value={selectedLanguages}
            languages={languages}
            context="trash"
          />
        </StyledSpaced>
      </StyledControls>

      {totalElements > 0 && (
        <StyledResultCount>
          <Tooltip
            title={
              allPageSelected
                ? t('translations_clear_selection')
                : t('translations_select_all')
            }
            disableInteractive
          >
            <Checkbox
              data-cy="trash-select-all-header"
              size="small"
              checked={allPageSelected}
              indeterminate={somePageSelected}
              onChange={onSelectAll}
            />
          </Tooltip>
          <StyledCount variant="body2">
            <T keyName="trash_key_count" params={{ count: totalElements }} />
          </StyledCount>
        </StyledResultCount>
      )}
    </>
  );
};
