import { Menu, MenuProps } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useEffect } from 'react';

import { useTranslationsDispatch } from '../context/TranslationsContext';
import { CompactMenuItem } from './FiltersComponents';
import { useActiveFilters } from './useActiveFilters';
import { useFiltersContent } from './useFiltersContent';

type Props = {
  anchorEl: MenuProps['anchorEl'];
  onClose: () => void;
};

export const FiltersMenu: React.FC<Props> = ({ anchorEl, onClose }) => {
  const dispatch = useTranslationsDispatch();
  const filtersContent = useFiltersContent();
  const activeFilters = useActiveFilters();
  const { t } = useTranslate();

  const handleClearFilters = () => {
    dispatch({ type: 'SET_FILTERS', payload: {} });
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
