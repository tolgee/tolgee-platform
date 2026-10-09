import { useRef } from 'react';
import { useField } from 'formik';
import { Box, FormHelperText, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import {
  StyledContainer,
  StyledInputLabel,
} from 'tg.component/common/TextField';
import { Tag } from 'tg.views/projects/translations/Tags/Tag';
import { TagInput } from 'tg.views/projects/translations/Tags/TagInput';

const StyledTags = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  min-height: 40px;
  padding: ${({ theme }) => theme.spacing(0.75, 1)};
  border: 1px solid
    ${({ theme }) =>
      theme.palette.tokens._components.input.outlined.enabledBorder};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  cursor: text;

  &:hover {
    border-color: ${({ theme }) =>
      theme.palette.tokens._components.input.outlined.hoverBorder};
  }

  &:focus-within {
    border-color: ${({ theme }) => theme.palette.primary.main};
    box-shadow: inset 0 0 0 1px ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledTagInput = styled(TagInput)`
  &&,
  &&:hover,
  &&:focus-within {
    background: transparent;
    border: 0;
    padding: ${({ theme }) => theme.spacing(0, 0.5)};
    flex-grow: 1;
  }
`;

type TagsFieldProps = {
  name: 'tagsIn' | 'tagsNotIn';
  label: string;
  dataCy: 'export-tags-in-selector' | 'export-tags-not-in-selector';
};

const TagsField = ({ name, label, dataCy }: TagsFieldProps) => {
  const { t } = useTranslate();
  const [field, , helper] = useField<string[]>(name);
  const tags = field.value;
  const tagsRef = useRef<HTMLDivElement>(null);

  const handleAdd = (tag: string) => {
    if (!tags.includes(tag)) {
      helper.setValue([...tags, tag]);
    }
  };

  const handleDelete = (tag: string) => {
    helper.setValue(tags.filter((existing) => existing !== tag));
  };

  return (
    <StyledContainer data-cy={dataCy}>
      <StyledInputLabel>{label}</StyledInputLabel>
      <StyledTags
        onClick={() => tagsRef.current?.querySelector('input')?.focus()}
        ref={tagsRef}
      >
        {tags.map((tag) => (
          <Tag key={tag} name={tag} onDelete={() => handleDelete(tag)} />
        ))}
        <StyledTagInput
          onAdd={handleAdd}
          filtered={tags}
          canAddNew={(value) => value.includes('*')}
          newOptionLabel={(pattern) =>
            t('export_translations_tags_pattern_option', 'Match "{pattern}"', {
              pattern,
            })
          }
          placeholder={t('export_translations_tags_placeholder', 'Add tag...')}
        />
      </StyledTags>
    </StyledContainer>
  );
};

export const TagsSelector = () => {
  const { t } = useTranslate();

  return (
    <Box display="grid" gap={1}>
      <Box
        display="grid"
        gridTemplateColumns="minmax(0, 1fr) minmax(0, 1fr)"
        alignItems="start"
        columnGap={3}
        rowGap={1}
      >
        <TagsField
          name="tagsIn"
          label={t(
            'export_translations_tags_in_label',
            'Include keys with tags'
          )}
          dataCy="export-tags-in-selector"
        />
        <TagsField
          name="tagsNotIn"
          label={t(
            'export_translations_tags_not_in_label',
            'Exclude keys with tags'
          )}
          dataCy="export-tags-not-in-selector"
        />
      </Box>
      <FormHelperText sx={{ mt: -0.5 }}>
        {t(
          'export_translations_tags_hint',
          'Leave empty to include all keys. Use * as a wildcard, e.g. feature-*'
        )}
      </FormHelperText>
    </Box>
  );
};
