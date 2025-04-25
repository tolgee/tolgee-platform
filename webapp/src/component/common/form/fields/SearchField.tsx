import React, { ComponentProps, useEffect, useState } from 'react';
import { IconButton, InputAdornment, TextField, useTheme } from '@mui/material';
import { SearchSm, XClose } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import { useDebounce } from 'use-debounce';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';

const SearchField = (
  props: {
    initial?: string;
    onSearch?: (value: string) => void;
    clearCallbackRef?: React.MutableRefObject<(() => void) | null>;
  } & ComponentProps<typeof TextField>
) => {
  const [search, setSearch] = useState(props.initial || '');
  const [debouncedSearch] = useDebounce(search, 500);
  const theme = useTheme();
  const { t } = useTranslate();

  const { onSearch, clearCallbackRef, ...otherProps } = props;

  useEffect(() => {
    if (debouncedSearch !== props.initial) {
      onSearch?.(debouncedSearch);
    }
  }, [debouncedSearch]);

  if (clearCallbackRef !== undefined) {
    clearCallbackRef.current = () => {
      setSearch('');
      onSearch?.('');
    };
  }

  return (
    <TextField
      data-cy="global-search-field"
      placeholder={t('standard_search_label')}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchSm width={20} height={20} />
          </InputAdornment>
        ),
        endAdornment: Boolean(search) && (
          <InputAdornment
            position="end"
            style={{ marginRight: -5, marginLeft: 5 }}
          >
            <IconButton
              size="small"
              onClick={stopAndPrevent(() => setSearch(''))}
              onMouseDown={stopAndPrevent()}
              edge="start"
            >
              <XClose />
            </IconButton>
          </InputAdornment>
        ),
        style: {
          paddingLeft: 12,
          fontSize: theme.typography.body2.fontSize,
          height: 40,
        },
      }}
      value={search}
      style={{ flexBasis: 200 }}
      {...otherProps}
      onChange={(e) => setSearch(e.target.value)}
    />
  );
};

export default SearchField;
