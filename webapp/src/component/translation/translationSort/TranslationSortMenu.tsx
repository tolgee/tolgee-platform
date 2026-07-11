import { Menu, MenuItem, MenuProps } from '@mui/material';
import { SortOption, useSortOptions } from './useSortOptions';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { useTranslate } from '@tolgee/react';

type Props = {
  value: string;
  onChange: (value: string) => void;
  anchorEl: MenuProps['anchorEl'];
  onClose: () => void;
  options?: SortOption[];
};

export const TranslationSortMenu = ({
  value,
  onChange,
  anchorEl,
  onClose,
  options: customOptions,
}: Props) => {
  const defaultOptions = useSortOptions();
  const options = customOptions ?? defaultOptions;
  const { t } = useTranslate();
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
      <CompactListSubheader sx={{ pt: 0 }}>
        {t('translation_sort_menu_label')}
      </CompactListSubheader>
      {options.map((o) => (
        <MenuItem
          value={o.value}
          key={o.value}
          onClick={() => {
            onChange(o.value);
            onClose();
          }}
          selected={o.value === value}
          data-cy="translation-controls-sort-item"
        >
          {o.label}
        </MenuItem>
      ))}
    </Menu>
  );
};
