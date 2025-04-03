import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Box, IconButton, Menu, styled } from '@mui/material';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';
import { components } from 'tg.service/apiSchema.generated';
import { UseInfiniteQueryResult } from 'react-query';
import { ApiError } from 'tg.service/http/ApiError';
import { FakeInput } from 'tg.component/FakeInput';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { XClose } from '@untitled-ui/icons-react';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { TextField } from 'tg.component/common/TextField';

const StyledClearButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

type PagedResult = {
  _embedded?: any;
  page?: components['schemas']['PageMetadata'];
};

type Props<T, S> = {
  items?: T[];
  selected?: S[];
  queryResult: UseInfiniteQueryResult<PagedResult, ApiError>;
  itemKey: (item: T) => React.Key;
  search: string;
  onClearSelected?: () => void;
  onSearchChange?: (search: string) => void;
  onFetchMore?: () => void;
  renderItem?: (
    props: React.HTMLAttributes<HTMLLIElement> & {
      key: any;
    },
    item: T
  ) => React.ReactNode;
  labelItem: (item: S) => string;
  label?: string;
  error?: React.ReactNode;
  searchPlaceholder?: string;
  displaySearch?: boolean;
  disabled?: boolean;
  minHeight?: boolean;
};

export function InfiniteMultiSearchSelect<T, S>({
  items,
  selected,
  queryResult,
  itemKey,
  search,
  onClearSelected,
  onSearchChange,
  onFetchMore,
  renderItem,
  labelItem,
  label,
  error,
  searchPlaceholder,
  displaySearch,
  disabled,
  minHeight,
}: Props<T, S>) {
  const [showSearchHint, setShowSearchHint] = useState(false);

  const renderOption =
    renderItem ??
    ((_, item: T) => {
      return itemKey(item);
    });

  const totalItems = queryResult?.data?.pages[0]?.page?.totalElements;

  useEffect(() => {
    if (!showSearchHint && (totalItems ?? 0) > 10) {
      setShowSearchHint(true);
    }
  }, [totalItems]);
  const showSearch =
    (displaySearch ?? true) && (showSearchHint || Boolean(search.length));

  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  const selectedItemsLabel = useMemo(() => {
    return selected?.map((v) => v && labelItem(v))?.join(', ');
  }, [selected]);

  const close = () => {
    setOpen(false);
    onSearchChange && onSearchChange('');
  };

  return (
    <>
      <TextField
        variant="outlined"
        value={selectedItemsLabel}
        label={label}
        disabled={disabled}
        error={!!error}
        helperText={error}
        minHeight={minHeight}
        InputProps={{
          placeholder: searchPlaceholder,
          onClick: () => setOpen(true),
          disabled: disabled,
          ref: anchorEl,
          fullWidth: true,
          sx: {
            cursor: 'pointer',
          },
          readOnly: true,
          inputComponent: FakeInput,
          margin: 'dense',
          endAdornment: (
            <Box sx={{ display: 'flex', marginRight: -0.5 }}>
              {Boolean(selected?.length && !disabled) && (
                <StyledClearButton
                  size="small"
                  onClick={stopAndPrevent(() => onClearSelected?.())}
                  tabIndex={-1}
                >
                  <XClose />
                </StyledClearButton>
              )}
              <StyledClearButton
                size="small"
                onClick={() => setOpen(true)}
                tabIndex={-1}
                sx={{ pointerEvents: 'none' }}
                disabled={disabled}
              >
                <ArrowDropDown />
              </StyledClearButton>
            </Box>
          ),
        }}
      />

      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          MenuListProps={{
            sx: { minWidth: anchorEl.current?.offsetWidth },
          }}
          onClose={close}
        >
          <SmoothProgress
            loading={Boolean(
              queryResult.isFetching && (search || totalItems === undefined)
            )}
            sx={{ position: 'absolute', top: 0, left: 0, right: 0 }}
          />
          {Boolean(totalItems !== undefined) && (
            <Box display="grid">
              <InfiniteSearchSelectContent
                open={true}
                onClose={close}
                anchorEl={anchorEl.current ?? undefined}
                items={items}
                itemKey={itemKey}
                displaySearch={showSearch}
                searchPlaceholder={searchPlaceholder}
                search={search}
                onSearch={onSearchChange}
                title={label}
                // compareFunction
                renderOption={renderOption}
                // ListboxProps={{ style: { maxHeight: 400, overflow: 'auto' } }}
                onGetMoreData={onFetchMore}
              />
            </Box>
          )}
        </Menu>
      )}
    </>
  );
}
