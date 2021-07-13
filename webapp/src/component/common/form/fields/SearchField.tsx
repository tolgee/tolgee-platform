import React, { ComponentProps, useEffect, useState } from 'react';
import { InputAdornment, TextField } from '@material-ui/core';
import { Search } from '@material-ui/icons';
import { T } from '@tolgee/react';
import { useDebounce } from 'use-debounce/lib';

const SearchField = (
  props: {
    initial?: string;
    onSearch: (value: string) => void;
  } & ComponentProps<typeof TextField>
) => {
  const [search, setSearch] = useState(props.initial || '');
  const [debouncedSearch] = useDebounce(search, 500);

  const { onSearch, ...otherProps } = props;

  useEffect(() => {
    if (debouncedSearch !== (props.initial || '')) {
      onSearch(debouncedSearch);
    }
  }, [debouncedSearch]);

  return (
    <TextField
      type="search"
      data-cy="global-search-field"
      label={<T>standard_search_label</T>}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <Search />
          </InputAdornment>
        ),
      }}
      value={search}
      {...otherProps}
      onChange={(e) => setSearch(e.target.value)}
    />
  );
};

export default SearchField;
