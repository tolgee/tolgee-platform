import React, { useState } from 'react';
import {
  Autocomplete,
  Box,
  IconButton,
  Tooltip,
  FormControl,
} from '@mui/material';
import { Add } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import { SelectItem } from './SearchSelect';
import {
  StyledWrapper,
  StyledHeading,
  StyledInput,
  StyledInputContent,
  StyledInputWrapper,
  StyledCompactMenuItem,
} from './SearchStyled';

function PopperComponent(props) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

function PaperComponent(props) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

const defaultCompareFunction = (prompt: string, label: string) => {
  return label.toLowerCase().startsWith(prompt.toLocaleLowerCase());
};

type Props<T> = {
  open: boolean;
  onClose?: () => void;
  onSelect?: (value: T) => void;
  anchorEl?: HTMLElement;
  selected: string | undefined;
  onAddNew?: (searchValue: string) => void;
  items: SelectItem<T>[];
  displaySearch?: boolean;
  searchPlaceholder?: string;
  title?: string;
  addNewTooltip?: string;
  minWidth?: number | string;
  compareFunction?: (prompt: string, label: string) => boolean;
};

export function SearchSelectContent<T extends React.Key>({
  open,
  onClose,
  onSelect,
  anchorEl,
  selected,
  onAddNew,
  items,
  displaySearch,
  searchPlaceholder,
  title,
  addNewTooltip,
  minWidth = 250,
  compareFunction,
}: Props<T>) {
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();

  const handleAddNew = () => {
    onAddNew?.(inputValue);
  };

  const width =
    !anchorEl || anchorEl.offsetWidth < minWidth
      ? minWidth
      : anchorEl.offsetWidth;

  return (
    <StyledWrapper sx={{ minWidth: width, maxWidth: width }}>
      <FormControl>
        <Autocomplete
          open
          filterOptions={(options, state) => {
            return options.filter((o) =>
              (compareFunction || defaultCompareFunction)(
                state.inputValue || '',
                o.name || ''
              )
            );
          }}
          options={items || []}
          inputValue={inputValue}
          onClose={(_, reason) => reason === 'escape' && onClose?.()}
          clearOnEscape={false}
          noOptionsText={t('global_nothing_found')}
          loadingText={t('global_loading_text')}
          isOptionEqualToValue={(o, v) => o.value === v.value}
          onInputChange={(_, value, reason) =>
            reason === 'input' && setInputValue(value)
          }
          getOptionLabel={({ name }) => name}
          PopperComponent={PopperComponent}
          PaperComponent={PaperComponent}
          renderOption={(props, option) => (
            <StyledCompactMenuItem
              key={option.value}
              {...props}
              selected={option.value === selected}
              data-cy="search-select-item"
            >
              <StyledInputContent>{option.name}</StyledInputContent>
            </StyledCompactMenuItem>
          )}
          onChange={(_, newValue) => {
            onSelect?.(newValue!.value);
            onClose?.();
          }}
          ListboxProps={{ style: { padding: 0 } }}
          renderInput={(params) => (
            <StyledInputWrapper>
              <StyledInput
                data-cy="search-select-search"
                key={Number(open)}
                sx={{ display: displaySearch ? undefined : 'none' }}
                ref={params.InputProps.ref}
                inputProps={params.inputProps}
                autoFocus
                placeholder={searchPlaceholder}
              />
              {!displaySearch && title && (
                <StyledHeading>{title}</StyledHeading>
              )}

              {onAddNew && (
                <Tooltip title={addNewTooltip || ''}>
                  <IconButton
                    size="small"
                    onClick={handleAddNew}
                    sx={{ ml: 0.5 }}
                    data-cy="search-select-new"
                  >
                    <Add />
                  </IconButton>
                </Tooltip>
              )}
            </StyledInputWrapper>
          )}
        />
      </FormControl>
    </StyledWrapper>
  );
}
