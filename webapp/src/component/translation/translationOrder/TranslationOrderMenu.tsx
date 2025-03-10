import { Menu, MenuItem, MenuProps } from '@mui/material';
import { useOrderOptions } from './useOrderOptions';

type Props = {
  value: string;
  onChange: (value: string) => void;
  anchorEl: MenuProps['anchorEl'];
  onClose: () => void;
};

export const TranslationOrderMenu = ({
  value,
  onChange,
  anchorEl,
  onClose,
}: Props) => {
  const options = useOrderOptions();
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
      {options.map((o) => (
        <MenuItem
          value={o.value}
          key={o.value}
          onClick={() => {
            onChange(o.value);
            onClose();
          }}
          selected={o.value === value}
        >
          {o.label}
        </MenuItem>
      ))}
    </Menu>
  );
};
