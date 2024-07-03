import { T, useTranslate } from '@tolgee/react';
import { XClose } from '@untitled-ui/icons-react';
import {
  Select,
  Typography,
  IconButton,
  Tooltip,
  styled,
  SxProps,
} from '@mui/material';
import { useState, useEffect } from 'react';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { FiltersType, LanguageModel } from './tools';
import { FilterOptions, useAvailableFilters } from './useAvailableFilters';
import { FilterType } from './tools';
import { getActiveFilters } from './getActiveFilters';
import { useFiltersContent } from './useFiltersContent';

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
`;

const StyledInputText = styled(Typography)`
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

type Props = {
  onChange: (value: FiltersType) => void;
  value: FiltersType;
  selectedLanguages: LanguageModel[];
  placeholder?: React.ReactNode;
  filterOptions?: FilterOptions;
  sx?: SxProps;
  className?: string;
};

export const TranslationFilters = ({
  value,
  onChange,
  selectedLanguages,
  placeholder,
  filterOptions,
  sx,
  className,
}: Props) => {
  const { t } = useTranslate();
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (isOpen) {
      filtersContent.refresh();
    }
  }, [isOpen]);

  const activeFilters = getActiveFilters(value);

  const { availableFilters } = useAvailableFilters(
    selectedLanguages,
    filterOptions
  );

  const findOption = (value: string) =>
    availableFilters
      .map((g) => g.options?.find((o) => o.value === value))
      .filter(Boolean)[0];

  const filtersContent = useFiltersContent(
    value,
    onChange,
    selectedLanguages,
    filterOptions
  );

  const handleClearFilters = (e) => {
    onChange({});
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
              opacity: value.length === 0 ? 0.5 : 1,
            }}
            variant="body2"
          >
            {value.length === 0
              ? placeholder ?? <T keyName="translations_filter_placeholder" />
              : (value.length === 1 && getFilterName(value[0])) || (
                  <T
                    keyName="translations_filters_text"
                    params={{ filtersNum: String(activeFilters.length) }}
                  />
                )}
          </StyledInputText>
          {Boolean(activeFilters.length) && (
            <Tooltip title={<T keyName="translations_filters_heading_clear" />}>
              <StyledClearButton
                size="small"
                onClick={stopAndPrevent(handleClearFilters)}
                onMouseDown={stopAndPrevent()}
                data-cy="translations-filter-clear-all"
              >
                <XClose width={20} height={20} />
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
      {...{ sx, className }}
    >
      {filtersContent.options}
    </StyledSelect>
  );
};
