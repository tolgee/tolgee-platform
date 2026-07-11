import React, { useEffect, useRef, useState } from 'react';
import {
  Checkbox,
  Autocomplete,
  Box,
  IconButton,
  Tooltip,
  FormControl,
  AutocompleteProps,
} from '@mui/material';
import { Plus } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { SelectItem } from 'tg.component/searchSelect/SearchSelect';
import {
  StyledWrapper,
  StyledHeading,
  StyledInput,
  StyledInputContent,
  StyledInputWrapper,
} from './SearchStyled';
import { CompactMenuItem } from '../ListComponents';

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

type RenderOption<T> = AutocompleteProps<
  SelectItem<T>,
  undefined,
  undefined,
  undefined
>['renderOption'];

type Props<T> = {
  open: boolean;
  onClose?: () => void;
  onSelect?: (value: T) => void;
  anchorEl?: HTMLElement;
  value: T[];
  onAction?: (searchValue: string) => void;
  items: SelectItem<T>[];
  displaySearch?: boolean;
  searchPlaceholder?: string;
  title?: string;
  actionTooltip?: string;
  actionIcon?: React.ReactNode;
  minWidth?: number | string;
  maxWidth?: number | string;
  renderOption?: RenderOption<T>;
};

export function SearchSelectMulti<T extends React.Key>({
  open,
  onClose,
  onSelect,
  anchorEl,
  value,
  onAction: onAddNew,
  items,
  displaySearch,
  searchPlaceholder,
  title,
  actionTooltip: addNewTooltip,
  actionIcon,
  minWidth,
  maxWidth,
  renderOption,
}: Props<T>) {
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();

  const handleAddNew = () => {
    onAddNew?.(inputValue);
  };

  const [fixedWidth, setFixedWidth] = useState<number>();

  const wrapperEl = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // let wrapper render and then fix it's width so it doesnt change when searching
    setTimeout(() => setFixedWidth(wrapperEl.current?.offsetWidth), 0);
  }, [items, minWidth, maxWidth]);

  const defaultRenderOption: RenderOption<T> = (props, option) => (
    <CompactMenuItem {...props} key={option.value} data-cy="search-select-item">
      <Checkbox
        size="small"
        edge="start"
        checked={value.includes(option.value)}
      />
      <StyledInputContent>{option.name}</StyledInputContent>
    </CompactMenuItem>
  );

  return (
    <StyledWrapper
      ref={wrapperEl}
      sx={{
        minWidth: fixedWidth || minWidth || anchorEl?.offsetWidth,
        maxWidth: fixedWidth || maxWidth,
      }}
    >
      <FormControl>
        <Autocomplete
          open
          filterOptions={(options, state) => {
            return options.filter((o) =>
              o.name
                .toLocaleLowerCase()
                .startsWith(state.inputValue?.toLocaleLowerCase())
            );
          }}
          options={items || []}
          inputValue={inputValue}
          onClose={(_, reason) => reason === 'escape' && onClose?.()}
          clearOnEscape={false}
          noOptionsText={t('global_nothing_found')}
          loadingText={t('global_loading_text')}
          isOptionEqualToValue={(o, v) => o.value === v.value}
          onInputChange={(_, value, reason) => {
            reason === 'input' && setInputValue(value);
          }}
          getOptionLabel={({ name }) => name}
          PopperComponent={PopperComponent}
          PaperComponent={PaperComponent}
          renderOption={renderOption || defaultRenderOption}
          onChange={(_, newValue) => {
            newValue?.value && onSelect?.(newValue.value);
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
                    {actionIcon || <Plus />}
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
