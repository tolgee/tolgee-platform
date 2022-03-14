import { Menu, MenuProps } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';

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
  const t = useTranslate();

  const handleClearFilters = () => {
    dispatch({ type: 'SET_FILTERS', payload: {} });
  };

  return (
    <Menu
      anchorEl={anchorEl}
      open={Boolean(anchorEl)}
      getContentAnchorEl={null}
      onClose={onClose}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
    >
      {filtersContent}
      {Boolean(activeFilters.length) && (
        <CompactMenuItem onClick={handleClearFilters}>
          {t('translations_filters_heading_clear')}
        </CompactMenuItem>
      )}
    </Menu>
  );
};
