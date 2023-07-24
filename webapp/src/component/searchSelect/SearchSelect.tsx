import { useRef, useState } from 'react';
import { Box, Select, styled } from '@mui/material';

import { SearchSelectPopover } from './SearchSelectPopover';
import { SearchSelectContent } from './SearchSelectContent';

export type SelectItem<T> = {
  value: T;
  name: string;
};

const StyledInputContent = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: -5px;
  contain: size;
  height: 23px;
`;

type Props<T> = {
  onChange?: (value: T) => void;
  onSelect?: (value: T) => void;
  anchorEl?: HTMLElement;
  items: SelectItem<T>[];
  value: T | undefined;
  onAddNew?: (searchValue: string) => void;
  searchPlaceholder?: string;
  title?: string;
  addNewTooltip?: string;
  displaySearch?: boolean;
  popperMinWidth?: string | number;
  renderValue?: (value: T | undefined) => React.ReactNode;
  SelectProps?: React.ComponentProps<typeof Select>;
  compareFunction?: (prompt: string, label: string) => boolean;
};

export function SearchSelect<T extends React.Key>({
  onChange,
  onSelect,
  items,
  value,
  onAddNew,
  searchPlaceholder,
  title,
  addNewTooltip,
  displaySearch = true,
  popperMinWidth,
  renderValue,
  SelectProps,
  compareFunction,
}: Props<T>) {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleOpen = () => {
    setIsOpen(true);
  };

  const handleSelect = (newValue: T) => {
    onSelect?.(newValue);
    onChange?.(newValue);
  };

  const myRenderValue = () => (
    <StyledInputContent>
      {renderValue
        ? renderValue(value)
        : (valueItem ? valueItem.name : value) || ''}
    </StyledInputContent>
  );

  const handleOnAddNew = (searchValue: string) => {
    setIsOpen(false);
    onAddNew?.(searchValue);
  };

  const valueItem = items.find((i) => i.value === value);

  return (
    <Box display="flex" data-cy="search-select">
      <Select
        ref={anchorEl}
        onOpen={handleOpen}
        fullWidth
        MenuProps={{ variant: 'menu' }}
        open={false}
        value={myRenderValue()}
        displayEmpty
        multiple
        renderValue={myRenderValue}
        {...SelectProps}
      />

      <SearchSelectPopover
        open={isOpen}
        onClose={handleClose}
        anchorEl={anchorEl.current!}
      >
        <SearchSelectContent
          open={isOpen}
          onClose={handleClose}
          selected={value as string | undefined}
          onSelect={handleSelect}
          anchorEl={anchorEl.current!}
          items={items}
          onAddNew={onAddNew ? handleOnAddNew : undefined}
          displaySearch={displaySearch}
          searchPlaceholder={searchPlaceholder}
          title={title}
          addNewTooltip={addNewTooltip}
          minWidth={popperMinWidth}
          compareFunction={compareFunction}
        />
      </SearchSelectPopover>
    </Box>
  );
}
