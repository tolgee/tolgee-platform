import React, { useState } from 'react';
import { Autocomplete, Box, FormControl } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import {
  StyledWrapper,
  StyledHeading,
  StyledInput,
  StyledInputWrapper,
} from 'tg.component/searchSelect/SearchStyled';

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

type Props<T extends { id: React.Key }> = {
  open: boolean;
  onClose?: () => void;
  anchorEl?: HTMLElement;
  items: T[];
  displaySearch?: boolean;
  searchPlaceholder?: string;
  title?: string;
  minWidth?: number;
  maxWidth?: number;
  compareFunction: (prompt: string, item: T) => boolean;
  renderOption: (
    props: React.HTMLAttributes<HTMLLIElement> & {
      key: any;
    },
    option: T
  ) => React.ReactNode;
  getOptionLabel: (item: T) => string;
};

export function InfiniteSearchSelectContent<T extends { id: React.Key }>({
  open,
  onClose,
  anchorEl,
  items,
  displaySearch,
  searchPlaceholder,
  title,
  minWidth = 250,
  maxWidth,
  compareFunction,
  renderOption,
  getOptionLabel,
}: Props<T>) {
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();

  const width =
    !anchorEl || anchorEl.offsetWidth < minWidth
      ? minWidth
      : anchorEl.offsetWidth;

  return (
    <StyledWrapper sx={{ minWidth: width, maxWidth: maxWidth ?? width }}>
      <FormControl>
        <Autocomplete
          open
          filterOptions={(options, state) => {
            return options.filter((o) =>
              compareFunction(state.inputValue || '', o)
            );
          }}
          options={items || []}
          inputValue={inputValue}
          onClose={(_, reason) => reason === 'escape' && onClose?.()}
          clearOnEscape={false}
          noOptionsText={t('global_nothing_found')}
          loadingText={t('global_loading_text')}
          onInputChange={(_, value, reason) => {
            reason === 'input' && setInputValue(value);
          }}
          getOptionLabel={getOptionLabel}
          PopperComponent={PopperComponent}
          PaperComponent={PaperComponent}
          renderOption={(props, item) => (
            <React.Fragment key={item.id}>
              {renderOption(props, item)}
            </React.Fragment>
          )}
          ListboxProps={{ style: { padding: 0 } }}
          renderInput={(params) => (
            <StyledInputWrapper
              sx={{ display: !displaySearch && !title ? 'none' : undefined }}
            >
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
            </StyledInputWrapper>
          )}
        />
      </FormControl>
    </StyledWrapper>
  );
}
