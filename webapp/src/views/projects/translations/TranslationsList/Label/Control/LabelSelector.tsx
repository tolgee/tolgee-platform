import React, { useState } from 'react';
import { useProject } from 'tg.hooks/useProject';
import { Autocomplete, MenuItem, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { CloseButton } from 'tg.views/projects/translations/Tags/CloseButton';
import { useLabels } from 'tg.hooks/useLabels';
import { useTranslate } from '@tolgee/react';
import { CustomPopper } from 'tg.views/projects/translations/Tags/CustomPopper';
import { TranslationLabel } from 'tg.component/TranslationLabel';

type LabelModel = components['schemas']['LabelModel'];

const StyledInput = styled('input')`
  display: flex;
  border: 0;
  background: transparent;
  padding: 0 4px;
  outline: 0;
  min-width: 0;
  width: 100%;
  flex-shrink: 1;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledInputWrapper = styled('div')`
  display: flex;
  align-items: center;
  width: 150px;
  overflow: hidden;
  padding: 0 6px;
`;

const StyledOption = styled('div')`
  overflow: hidden;
  white-space: nowrap;
`;

export const LabelSelector: React.FC<{
  existing?: LabelModel[];
  onClose?: () => void;
  onSelect?: (labelId: number) => void;
}> = ({ existing, onClose, onSelect }) => {
  const { t } = useTranslate();
  const [value, setValue] = useState<string>();
  const project = useProject();
  const { labels, loadableList, setSearch } = useLabels({
    projectId: project.id,
  });

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
    .filter((label) => {
      return !(existing && existing.some((l) => l.id === label.id));
    })
    .map((label) => ({
      label: label.name,
      value: label.id,
      model: label,
    }));

  return (
    <Autocomplete
      data-cy="label-selector-autocomplete"
      className="label-autocomplete"
      onInputChange={(_, value) => {
        setSearch(value);
        setValue(value);
      }}
      PopperComponent={CustomPopper}
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
            data-cy="autocomplete-label-input"
            size={0}
            value={value || ''}
            placeholder={t('translations_filters_labels_search_placeholder')}
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
        setValue('');
        setSearch('');
      }}
      renderOption={(attrs, option) => {
        return (
          <MenuItem
            {...attrs}
            onClick={(e) => {
              e.stopPropagation();
              if (attrs.onClick) {
                attrs.onClick(e);
              }
            }}
          >
            <StyledOption data-cy="label-autocomplete-option">
              <TranslationLabel label={option.model} />
            </StyledOption>
          </MenuItem>
        );
      }}
      options={options || []}
    />
  );
};
