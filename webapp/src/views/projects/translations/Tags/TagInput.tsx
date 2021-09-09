import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core';
import { Autocomplete } from '@material-ui/lab';
import { useDebounce } from 'use-debounce';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { Wrapper } from './Wrapper';
import { CloseButton } from './CloseButton';
import { CustomPopper } from './CustomPopper';

const useStyles = makeStyles({
  autocomplete: {
    display: 'flex',
    alignItems: 'center',
    width: 150,
    overflow: 'hidden',
  },
  input: {
    display: 'flex',
    border: 0,
    background: 'transparent',
    padding: '0px 4px',
    outline: 0,
    minWidth: 0,
    width: '100%',
    fontSize: 14,
    flexShrink: 1,
  },
  option: {
    whiteSpace: 'nowrap',
    overflow: 'hidden',
  },
  listbox: {
    padding: 0,
  },
});

type Props = {
  onClose?: () => void;
  onAdd: (name: string) => void;
  className?: string;
  autoFocus?: boolean;
  existing?: string[];
  placeholder?: string;
};

export const TagInput: React.FC<Props> = ({
  onClose,
  onAdd,
  className,
  autoFocus,
  existing,
  placeholder,
}) => {
  const classes = useStyles();
  const [value, setValue] = useState('');
  const [search] = useDebounce(value, 500);

  const project = useProject();

  const tags = useApiQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: { projectId: project.id },
    query: {
      search,
    },
    options: {
      cacheTime: 0,
      keepPreviousData: true,
    },
    fetchOptions: { disableNotFoundHandling: true },
  });

  const handleKeyUp = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Escape') {
      onClose?.();
    }
  };

  const options = (tags.data?._embedded?.tags?.map(({ name }) => name) || [])
    .concat(existing || [])
    .filter((value, index, self) => {
      return self.indexOf(value) === index;
    })
    .map((tag) => ({
      label: tag,
      value: tag,
      translation: '',
    }));

  return (
    <Wrapper role="input" className={className}>
      <Autocomplete
        loading={tags.isFetching}
        className={classes.autocomplete}
        autoHighlight
        noOptionsText={<T>translations_tags_no_results</T>}
        PopperComponent={CustomPopper}
        options={options}
        filterOptions={(options) => {
          const filtered = options.filter((o) => o.value.startsWith(search));
          if (search !== '' && !options.find((item) => item.value === search)) {
            filtered.push({
              value: search,
              label: '',
              translation: 'translations_tag_create',
            });
          }
          return filtered;
        }}
        inputValue={value}
        onInputChange={(_, value) => {
          setValue(value);
        }}
        onChange={(_, newValue) => {
          if (newValue) {
            onAdd(newValue.value);
          }
        }}
        getOptionLabel={() => ''}
        getOptionSelected={() => true}
        renderOption={(option) => {
          return (
            <span data-cy="tag-autocomplete-option" className={classes.option}>
              {option.translation ? (
                <T parameters={{ tag: search }}>{option.translation}</T>
              ) : (
                option.label
              )}
            </span>
          );
        }}
        ListboxProps={{ style: { padding: 0 } }}
        renderInput={(params) => (
          <div className={classes.autocomplete} ref={params.InputProps.ref}>
            <input
              {...params.inputProps}
              data-cy="tag-autocomplete-input"
              size={0}
              className={classes.input}
              onKeyUp={handleKeyUp}
              autoFocus={autoFocus}
              placeholder={placeholder}
            />
            {onClose && <CloseButton onClick={onClose} />}
          </div>
        )}
      />
    </Wrapper>
  );
};
