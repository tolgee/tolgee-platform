import { T, useTranslate } from '@tolgee/react';
import { Clear } from '@mui/icons-material';
import {
  Select,
  Typography,
  IconButton,
  Tooltip,
  useTheme,
  styled,
} from '@mui/material';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { useAvailableFilters } from './useAvailableFilters';
import { FilterType } from './tools';
import { useActiveFilters } from './useActiveFilters';
import { useFiltersContent } from './useFiltersContent';
import { useState } from 'react';
import { useEffect } from 'react';

const StyledWrapper = styled('div')`
  display: flex;
  align-items: center;
`;

const StyledSelect = styled(Select)`
  height: 40px;
  margin-top: 0px;
  margin-bottom: 0px;
  display: flex;
  align-items: stretch;
  width: 200px;
  & div:focus {
    background-color: transparent;
  }
  & .MuiSelect-select {
    padding-top: 0px;
    padding-bottom: 0px;
    display: flex;
    align-items: center;
    overflow: hidden;
    position: relative;
  }
`;

const StyledInputContent = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  overflow: hidden;
`;

const StyledInputText = styled(Typography)`
  margin-top: 2px;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  flex-grow: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledClearButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

export const Filters = () => {
  const { t } = useTranslate();
  const { setFilters } = useTranslationsActions();
  const selectedLanguages = useTranslationsSelector((v) => v.selectedLanguages);
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (isOpen) {
      filtersContent.refresh();
    }
  }, [isOpen]);

  const activeFilters = useActiveFilters();

  const theme = useTheme();

  const { availableFilters } = useAvailableFilters(selectedLanguages);

  const findOption = (value: string) =>
    availableFilters
      .map((g) => g.options?.find((o) => o.value === value))
      .filter(Boolean)[0];

  const filtersContent = useFiltersContent();

  const handleClearFilters = (e) => {
    setFilters({});
  };

  function getFilterName(value) {
    const option = findOption(value);
    if (option?.label) {
      return option.label;
    }

    const parsed = JSON.parse(value) as FilterType;
    if (parsed.filter === 'filterNamespace') {
      return (parsed.value as string) || t('namespace_default');
    }
  }

  return (
    <StyledWrapper>
      <StyledSelect
        onOpen={() => setIsOpen(true)}
        onClose={() => setIsOpen(false)}
        variant="outlined"
        value={activeFilters}
        data-cy="translations-filter-select"
        renderValue={(value: any) => (
          <StyledInputContent>
            <StyledInputText
              style={{
                color:
                  value.length === 0 ? '#8b9097' : theme.palette.text.primary,
              }}
              variant="body2"
            >
              {value.length === 0 ? (
                <T>translations_filter_placeholder</T>
              ) : (
                (value.length === 1 && getFilterName(value[0])) || (
                  <T params={{ filtersNum: String(activeFilters.length) }}>
                    translations_filters_text
                  </T>
                )
              )}
            </StyledInputText>
            {Boolean(activeFilters.length) && (
              <Tooltip title={<T noWrap>translations_filters_heading_clear</T>}>
                <StyledClearButton
                  size="small"
                  onClick={stopAndPrevent(handleClearFilters)}
                  onMouseDown={stopAndPrevent()}
                  data-cy="translations-filter-clear-all"
                >
                  <Clear fontSize="small" />
                </StyledClearButton>
              </Tooltip>
            )}
          </StyledInputContent>
        )}
        MenuProps={{
          variant: 'menu',
        }}
        margin="dense"
        displayEmpty
        multiple
      >
        {filtersContent.options}
      </StyledSelect>
    </StyledWrapper>
  );
};
