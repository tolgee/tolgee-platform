import { useRef } from 'react';
import { useField } from 'formik';
import { Box, styled, Tooltip, Typography } from '@mui/material';
import { InfoCircle } from '@untitled-ui/icons-react';
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

const StyledInfoIcon = styled(InfoCircle)`
  width: 16px;
  height: 16px;
  color: ${({ theme }) => theme.palette.text.secondary};
  cursor: help;
`;

type TagsFieldName = 'tagsIn' | 'tagsNotIn';

type TagsFieldProps = {
  name: TagsFieldName;
  hideTagsUsedIn: TagsFieldName;
  label: string;
  dataCy: 'export-tags-in-selector' | 'export-tags-not-in-selector';
};

const TagsField = ({ name, hideTagsUsedIn, label, dataCy }: TagsFieldProps) => {
  const { t } = useTranslate();
  const [field, , helper] = useField<string[]>(name);
  const [otherField] = useField<string[]>(hideTagsUsedIn);
  const tags = field.value;
  const usedTags = [...tags, ...otherField.value];
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
          filtered={usedTags}
          canAddNew={(value) =>
            value.includes('*') && !usedTags.includes(value)
          }
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
      <Box display="flex" alignItems="center" gap={0.5}>
        <Typography variant="body2" fontWeight={500}>
          {t('export_translations_tags_title', 'Tags')}
        </Typography>
        <Tooltip
          title={
            <span data-cy="export-tags-hint">
              {t(
                'export_translations_tags_hint',
                'Use * to match several tags at once. For example, release-* matches release-1.0, release-2.0 and every other tag that starts with release-. Leave both fields empty to export all keys.'
              )}
            </span>
          }
        >
          <span style={{ display: 'inline-flex' }} data-cy="export-tags-info">
            <StyledInfoIcon />
          </span>
        </Tooltip>
      </Box>
      <Box
        display="grid"
        gridTemplateColumns="minmax(0, 1fr) minmax(0, 1fr)"
        alignItems="start"
        columnGap={3}
        rowGap={1}
      >
        <TagsField
          name="tagsIn"
          hideTagsUsedIn="tagsNotIn"
          label={t(
            'export_translations_tags_in_label',
            'Include keys with tags'
          )}
          dataCy="export-tags-in-selector"
        />
        <TagsField
          name="tagsNotIn"
          hideTagsUsedIn="tagsIn"
          label={t(
            'export_translations_tags_not_in_label',
            'Exclude keys with tags'
          )}
          dataCy="export-tags-not-in-selector"
        />
      </Box>
    </Box>
  );
};
