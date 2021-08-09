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
    flexShrink: 1,
    flexBasis: '100%',
  },
  input: {
    display: 'flex',
    border: 0,
    background: 'transparent',
    padding: 0,
    outline: 0,
    minWidth: 0,
    width: '100%',
    flexBasis: 150,
    fontSize: 14,
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
  onClose: () => void;
  onAdd: (name: string) => void;
};

export const TagInput: React.FC<Props> = ({ onClose, onAdd }) => {
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
      onClose();
    }
  };

  return (
    <Wrapper role="input">
      <Autocomplete
        loading={tags.isFetching}
        className={classes.autocomplete}
        autoHighlight
        loadingText={<T>translations_tags_loading</T>}
        noOptionsText={<T>translations_tags_no_results</T>}
        PopperComponent={CustomPopper}
        options={
          tags.data?._embedded?.tags?.map((tag) => ({
            label: tag.name,
            value: tag.name,
            translation: '',
          })) || []
        }
        filterOptions={(options, params) => {
          const filtered = options.filter((o) =>
            o.value.startsWith(params.inputValue)
          );
          if (
            search !== '' &&
            search === value &&
            !options.find((item) => item.value === params.inputValue) &&
            !tags.isFetching
          ) {
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
        getOptionLabel={(option) => option.label}
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
              autoFocus
            />
            <CloseButton onClick={onClose} />
          </div>
        )}
      />
    </Wrapper>
  );
};
