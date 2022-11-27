import { useRef, useState } from 'react';
import { Box, Select } from '@mui/material';

import { SearchSelectPopover } from './SearchSelectPopover';

export type SelectItem = {
  value: string;
  name: string;
};

type Props = {
  onSelect: (value: string) => void;
  items: SelectItem[];
  value: string | undefined;
  onAddNew: (searchValue: string) => void;
  searchPlaceholder?: string;
  title?: string;
  addNewTooltip?: string;
  displaySearch?: boolean;
  SelectProps?: Partial<React.ComponentProps<typeof Select>>;
};

export const SearchSelect: React.FC<Props> = ({
  onSelect,
  items,
  value,
  onAddNew,
  searchPlaceholder,
  title,
  addNewTooltip,
  displaySearch = true,
}) => {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleOpen = () => {
    setIsOpen(true);
  };

  const handleSelect = (value: string) => {
    setIsOpen(false);
    onSelect(value);
  };

  const handleOnAddNew = (searchValue: string) => {
    setIsOpen(false);
    onAddNew(searchValue);
  };

  const valueItem = items.find((i) => i.value === value);

  return (
    <Box display="flex" data-cy="organization-switch" overflow="hidden">
      <Select
        ref={anchorEl}
        onOpen={handleOpen}
        size="small"
        fullWidth
        MenuProps={{ variant: 'menu' }}
        open={false}
        value=""
        displayEmpty
        renderValue={() => (valueItem ? valueItem.name : value) || ''}
      />

      <SearchSelectPopover
        open={isOpen}
        onClose={handleClose}
        selected={value}
        onSelect={handleSelect}
        anchorEl={anchorEl.current!}
        items={items}
        onAddNew={handleOnAddNew}
        displaySearch={displaySearch}
        searchPlaceholder={searchPlaceholder}
        title={title}
        addNewTooltip={addNewTooltip}
      />
    </Box>
  );
};
