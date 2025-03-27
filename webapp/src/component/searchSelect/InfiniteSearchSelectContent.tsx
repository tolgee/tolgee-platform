import React, { useRef, useState } from 'react';
import { Autocomplete, Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import {
  StyledHeading,
  StyledInput,
  StyledInputWrapper,
  StyledWrapper,
} from 'tg.component/searchSelect/SearchStyled';

const FETCH_NEXT_PAGE_SCROLL_THRESHOLD_IN_PIXELS = 220;

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

type Props<T> = {
  open: boolean;
  onClose?: () => void;
  anchorEl?: HTMLElement;
  items: T[] | undefined;
  itemKey: (item: T) => React.Key;
  displaySearch?: boolean;
  searchPlaceholder?: string;
  search?: string;
  onSearch?: (value: string) => void;
  title?: string;
  minWidth?: number;
  maxWidth?: number;
  onGetMoreData?: () => void;
  compareFunction?: (prompt: string, item: T) => boolean;
  renderOption: (
    props: React.HTMLAttributes<HTMLLIElement> & {
      key: any;
    },
    option: T
  ) => React.ReactNode;
  ListboxProps?: React.HTMLAttributes<HTMLUListElement>;
};

export function InfiniteSearchSelectContent<T>({
  open,
  onClose,
  anchorEl,
  items,
  itemKey,
  displaySearch,
  searchPlaceholder,
  search,
  onSearch,
  title,
  minWidth = 250,
  maxWidth,
  compareFunction,
  renderOption,
  ListboxProps,
  onGetMoreData,
}: Props<T>) {
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();
  const listboxEl = useRef<HTMLDivElement>(null);

  const width =
    !anchorEl || anchorEl.offsetWidth < minWidth
      ? minWidth
      : anchorEl.offsetWidth;

  return (
    <StyledWrapper sx={{ minWidth: width, maxWidth: maxWidth ?? width }}>
      <Autocomplete
        open
        filterOptions={(options, state) => {
          if (compareFunction) {
            return options.filter((o) =>
              compareFunction(state.inputValue || '', o)
            );
          } else {
            return options;
          }
        }}
        options={items || []}
        inputValue={onSearch ? search : inputValue}
        onClose={(_, reason) => reason === 'escape' && onClose?.()}
        clearOnEscape={false}
        noOptionsText={t('global_nothing_found')}
        loadingText={t('global_loading_text')}
        onInputChange={(_, value, reason) => {
          reason === 'input' && onSearch
            ? onSearch(value)
            : setInputValue(value);
        }}
        getOptionLabel={() => ''}
        PopperComponent={PopperComponent}
        PaperComponent={PaperComponent}
        renderOption={(props, item) => (
          <React.Fragment key={itemKey(item)}>
            {renderOption(props, item)}
          </React.Fragment>
        )}
        ListboxProps={{
          onScroll: (event) => {
            const target = event.target as HTMLDivElement;
            if (
              target.scrollHeight - target.clientHeight - target.scrollTop <
              FETCH_NEXT_PAGE_SCROLL_THRESHOLD_IN_PIXELS
            ) {
              onGetMoreData?.();
            }
          },
          ...ListboxProps,
          style: {
            overflow: 'auto',
            padding: 0,
            ...ListboxProps?.style,
          },
          ref: listboxEl,
        }}
        onKeyDown={(e) => e.stopPropagation()}
        renderInput={(params) => (
          <StyledInputWrapper
            sx={{
              display: !displaySearch && !title ? 'none' : undefined,
              marginBottom: 1,
            }}
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
            {!displaySearch && title && <StyledHeading>{title}</StyledHeading>}
          </StyledInputWrapper>
        )}
      />
    </StyledWrapper>
  );
}
