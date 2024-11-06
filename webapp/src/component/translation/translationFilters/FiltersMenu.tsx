import { Menu, MenuProps } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import React, { useEffect } from 'react';
import { CompactMenuItem } from 'tg.component/ListComponents';

import { getActiveFilters } from './getActiveFilters';
import { useFiltersContent } from './useFiltersContent';
import { FiltersType } from './tools';

type Props = {
  filters: FiltersType;
  onChange: (value: FiltersType) => void;
  onClose: () => void;
  anchorEl: MenuProps['anchorEl'];
  filtersContent: ReturnType<typeof useFiltersContent>;
};

export const FiltersMenu: React.FC<Props> = ({
  anchorEl,
  onClose,
  onChange,
  filtersContent,
  filters,
}) => {
  const activeFilters = getActiveFilters(filters);
  const { t } = useTranslate();

  const handleClearFilters = () => {
    onChange({});
  };

  useEffect(() => {
    if (anchorEl) {
      filtersContent.refresh();
    }
  }, [anchorEl]);

  return (
    <Menu
      anchorEl={anchorEl}
      open={Boolean(anchorEl)}
      onClose={onClose}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
    >
      {filtersContent.options}
      {Boolean(activeFilters.length) && (
        <CompactMenuItem onClick={handleClearFilters}>
          {t('translations_filters_heading_clear')}
        </CompactMenuItem>
      )}
    </Menu>
  );
};
