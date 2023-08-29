import React, { useState } from 'react';
import { Autocomplete, styled } from '@mui/material';
import { useDebounce } from 'use-debounce';
import { T, useTranslate } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { Wrapper } from './Wrapper';
import { CloseButton } from './CloseButton';
import { CustomPopper } from './CustomPopper';
import { MenuItem } from '@mui/material';

const StyledWrapper = styled(Wrapper)`
  & .autocomplete {
    display: flex;
    align-items: center;
    width: 150px;
    overflow: hidden;
  }
`;

const StyledInput = styled('input')`
  display: flex;
  border: 0px;
  background: transparent;
  padding: 0px 4px;
  outline: 0px;
  min-width: 0px;
  width: 100%;
  font-size: 14;
  flex-shrink: 1;
`;

const StyledOption = styled('span')`
  white-space: nowrap;
  overflow: hidden;
`;

type Props = {
  onClose?: () => void;
  onAdd: (name: string) => void;
  className?: string;
  autoFocus?: boolean;
  existing?: string[];
  filtered?: string[];
  placeholder?: string;
  noNew?: boolean;
};

export const TagInput: React.FC<Props> = ({
  onClose,
  onAdd,
  className,
  autoFocus,
  existing,
  filtered,
  placeholder,
  noNew,
}) => {
  const [value, setValue] = useState('');
  const [search] = useDebounce(value, 500);
  const { t } = useTranslate();

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
      new: false,
    }));

  return (
    <StyledWrapper role="input" className={className}>
      <Autocomplete
        className="autocomplete"
        loading={tags.isFetching}
        autoHighlight
        noOptionsText={
          <StyledOption>
            <T keyName="translations_tags_no_results" />
          </StyledOption>
        }
        PopperComponent={CustomPopper}
        options={options}
        filterOptions={(options) => {
          const result = options.filter(
            (o) =>
              !filtered?.includes(o.value) &&
              o.value.toLowerCase().startsWith(search.toLowerCase())
          );
          if (
            !noNew &&
            search !== '' &&
            !options.find((item) => item.value === search)
          ) {
            result.push({
              value: search,
              label: '',
              new: true,
            });
          }
          return result;
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
        isOptionEqualToValue={() => true}
        renderOption={(attrs, option) => {
          return (
            <MenuItem {...attrs}>
              <StyledOption data-cy="tag-autocomplete-option">
                {option.new
                  ? t('translations_tag_create', { tag: search })
                  : option.label}
              </StyledOption>
            </MenuItem>
          );
        }}
        ListboxProps={{ style: { padding: 0 } }}
        renderInput={(params) => (
          <div className="autocomplete" ref={params.InputProps.ref}>
            <StyledInput
              {...params.inputProps}
              data-cy="tag-autocomplete-input"
              size={0}
              onKeyUp={handleKeyUp}
              autoFocus={autoFocus}
              placeholder={placeholder}
            />
            {onClose && <CloseButton onClick={onClose} />}
          </div>
        )}
      />
    </StyledWrapper>
  );
};
