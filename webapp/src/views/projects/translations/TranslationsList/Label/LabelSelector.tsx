import React, { useEffect, useState } from 'react';
import { useDebounce } from 'use-debounce';
import { useProject } from 'tg.hooks/useProject';
import { Autocomplete, MenuItem, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { CloseButton } from 'tg.views/projects/translations/Tags/CloseButton';
import { useLabels } from 'tg.hooks/useLabels';

type LabelModel = components['schemas']['LabelModel'];

const StyledInput = styled('input')`
  display: flex;
  border: 0;
  background: transparent;
  padding: 0 4px;
  outline: 0;
  min-width: 0;
  width: 100%;
  font-size: 14px;
  flex-shrink: 1;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledInputWrapper = styled('div')`
  display: flex;
  align-items: center;
  width: 150px;
  overflow: hidden;
`;

export const LabelSelector: React.FC<{
  existing?: LabelModel[];
  onClose?: () => void;
  onSelect?: (labelId: number) => void;
}> = ({ existing, onClose, onSelect }) => {
  const [value, setValue] = useState<string>();
  const [inputValue, setInputValue] = useState<string>('');
  const project = useProject();
  const { labels, loadableList, setSearch, fetchList } = useLabels({
    projectId: project.id,
  });
  const [debouncedValue] = useDebounce(value, 500);

  useEffect(() => {
    fetchList();
    setSearch(debouncedValue || '');
  }, [debouncedValue, setSearch]);

  const handleScroll = async (event: React.UIEvent<HTMLUListElement>) => {
    const { scrollTop, scrollHeight, clientHeight } = event.currentTarget;
    if (
      scrollHeight - scrollTop === clientHeight &&
      loadableList.hasNextPage &&
      !loadableList.isFetching
    ) {
      await loadableList.fetchNextPage();
    }
  };

  const options = labels
    .concat(existing || [])
    .filter((value, index, self) => {
      return self.indexOf(value) === index;
    })
    .map((label) => ({
      label: label.name,
      value: label.id,
    }));

  return (
    <Autocomplete
      className="label-autocomplete"
      inputValue={inputValue}
      onInputChange={(_, value) => {
        setValue(value);
        setInputValue(value);
      }}
      ListboxProps={{
        onScroll: handleScroll,
        style: { maxHeight: 300, overflow: 'auto' },
      }}
      renderInput={(params) => (
        <StyledInputWrapper
          className="autocomplete"
          ref={params.InputProps.ref}
        >
          <StyledInput
            {...params.inputProps}
            data-cy="tag-autocomplete-input"
            size={0}
            onClick={(e) => {
              e.stopPropagation();
              if (params.inputProps.onClick) {
                params.inputProps.onClick(e);
              }
            }}
          />
          {onClose && <CloseButton onClick={onClose} />}
        </StyledInputWrapper>
      )}
      onChange={(_, newValue) => {
        if (newValue) {
          onSelect?.(newValue.value);
        }
        setInputValue('');
      }}
      renderOption={(attrs, option) => {
        return (
          <MenuItem {...attrs}>
            <div data-cy="label-autocomplete-option">{option.label}</div>
          </MenuItem>
        );
      }}
      options={options || []}
    />
  );
};
